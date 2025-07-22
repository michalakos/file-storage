package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.exception.FileStorageException;
import com.mvasilakos.filestorage.exception.StorageLimitExceededException;
import com.mvasilakos.filestorage.mapper.FileMetadataMapper;
import com.mvasilakos.filestorage.model.FileAccessLevel;
import com.mvasilakos.filestorage.model.FileMetadata;
import com.mvasilakos.filestorage.model.FilePermission;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.repository.FileMetadataRepository;
import com.mvasilakos.filestorage.repository.FilePermissionRepository;
import com.mvasilakos.filestorage.validator.FileValidator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


/**
 * File service.
 */
@Service
public class FileService {

  @Value("${file.storage.max-storage-per-user:1048576}")
  private long maxStoragePerUser;

  private final FileMetadataMapper fileMetadataMapper;
  private final FileMetadataRepository fileMetadataRepository;
  private final FilePermissionRepository filePermissionRepository;
  private final FileValidator fileValidator;
  private final UserService userService;
  private final Path rootLocation;

  /**
   * Constructor.
   *
   * @param fileMetadataRepository   fileMetadataRepository
   * @param fileMetadataMapper       fileMetadataMapper
   * @param filePermissionRepository filePermissionRepository
   * @param userService              userService
   * @param storagePath              storagePath
   */
  public FileService(
      FileMetadataRepository fileMetadataRepository,
      FileMetadataMapper fileMetadataMapper,
      FilePermissionRepository filePermissionRepository,
      FileValidator fileValidator,
      UserService userService,
      @Value("${storage.location}") String storagePath) {
    this.fileMetadataMapper = fileMetadataMapper;
    this.fileMetadataRepository = fileMetadataRepository;
    this.filePermissionRepository = filePermissionRepository;
    this.fileValidator = fileValidator;
    this.userService = userService;
    this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
    initStorage();
  }

  private void initStorage() {
    try {
      Files.createDirectories(rootLocation);
    } catch (IOException e) {
      throw new RuntimeException("Could not initialize storage", e);
    }
  }

  /**
   * Store file.
   *
   * @param file  file to store
   * @param owner the user who is storing the file
   * @return file metadata DTO
   * @throws FileStorageException if storage fails or limits are exceeded
   */
  public FileMetadataDto storeFile(MultipartFile file, User owner) {
    fileValidator.validateFile(file);

    long userStorageUsed = calculateUserTotalStorage(owner);
    long originalFileSize = file.getSize();
    validateStorageLimit(userStorageUsed, originalFileSize);

    FileMetadata metadata = createFileMetadata(file, owner);

    try {
      Path storagePath = rootLocation.resolve(metadata.getStoragePath());
      Files.createDirectories(storagePath.getParent());

      try (InputStream inputStream = file.getInputStream();
          OutputStream fileOutputStream = Files.newOutputStream(storagePath);
          GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {

        // Copy the file content through the compression stream
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          gzipOutputStream.write(buffer, 0, bytesRead);
        }
      }

      long compressedFileSize = Files.size(storagePath);
      metadata.setSize(compressedFileSize);
      metadata.setOriginalFileSize(originalFileSize);

      FileMetadata savedMetadata = fileMetadataRepository.save(metadata);
      return fileMetadataMapper.toDto(savedMetadata);

    } catch (IOException e) {
      Path storagePath = rootLocation.resolve(metadata.getStoragePath());
      try {
        if (Files.exists(storagePath)) {
          Files.delete(storagePath);
        }
      } catch (IOException deleteException) {
        System.err.println("Failed to clean up partially stored file: " + storagePath + " due to: "
            + deleteException.getMessage());
      }
      throw new FileStorageException("Failed to store file: " + file.getOriginalFilename(), e);
    }
  }

  private void validateStorageLimit(long userStorageUsed, long fileSize) {
    if (userStorageUsed + fileSize > maxStoragePerUser) {
      long availableBytes = maxStoragePerUser - userStorageUsed;

      throw new StorageLimitExceededException(
          String.format("User storage limit exceeded. Used: %s, Available: %s, File size: %s",
              formatBytes(userStorageUsed),
              formatBytes(availableBytes),
              formatBytes(fileSize))
      );
    }
  }

  /**
   * Format bytes into MB with appropriate decimal places.
   *
   * @param bytes the number of bytes
   * @return formatted string in MB
   */
  private String formatBytes(long bytes) {
    double megabytes = bytes / (1024.0 * 1024.0);

    if (megabytes >= 100) {
      return String.format("%.0f MB", megabytes); // >= 100 MB: no decimals
    } else if (megabytes >= 10) {
      return String.format("%.1f MB", megabytes); // >= 10 MB: 1 decimal
    } else {
      return String.format("%.2f MB", megabytes); // < 10 MB: 2 decimals
    }
  }

  private FileMetadata createFileMetadata(MultipartFile file, User owner) {
    UUID id = UUID.randomUUID();
    String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
    String storagePath = generateStoragePath(id, sanitizedFilename);

    return FileMetadata.builder()
        .id(id)
        .filename(sanitizedFilename)
        .contentType(file.getContentType())
        .size(file.getSize())
        .uploadDate(LocalDateTime.now())
        .storagePath(storagePath)
        .owner(owner)
        .build();
  }

  private String sanitizeFilename(String filename) {
    if (filename == null) {
      return "unknown";
    }
    // Remove path traversal characters and other potentially dangerous chars
    return filename.replaceAll("[^a-zA-Z0-9._-]", "_").trim();
  }

  private String generateStoragePath(UUID id, String filename) {
    return String.format("%s_%s", id, filename);
  }

  /**
   * Change the file's name.
   *
   * @param fileId      file id
   * @param newFilename new file name
   * @param user        user who wants to change the file name
   * @return file metadata
   */
  public FileMetadataDto renameFile(UUID fileId, String newFilename, User user) {
    FileMetadata metadata = fileMetadataRepository.findByIdAndOwner(fileId, user)
        .orElseThrow(() -> new RuntimeException("File not found"));
    metadata.setFilename(newFilename);
    fileMetadataRepository.save(metadata);
    return fileMetadataMapper.toDto(metadata);
  }

  /**
   * Get metadata for a file.
   *
   * @param fileId file id
   * @param user   user who wants to access the file
   * @return file metadata
   */
  public FileMetadataDto getFileMetadata(UUID fileId, User user) {
    FileMetadata metadata = fileMetadataRepository.findByIdAndOwnerOrSharedWith(fileId, user)
        .orElseThrow(() -> new RuntimeException("File not found"));
    return fileMetadataMapper.toDto(metadata);
  }

  /**
   * Get file data.
   *
   * @param fileId file id
   * @param user   user who wants to access the file
   * @return file content
   */
  public Resource loadFileAsResource(UUID fileId, User user) {
    FileMetadata metadata = fileMetadataRepository.findByIdAndOwnerOrSharedWith(fileId, user)
        .orElseThrow(() -> new FileStorageException("File not found for ID: " + fileId));
    Path filePath = rootLocation.resolve(metadata.getStoragePath());

    if (!Files.exists(filePath)) {
      throw new FileStorageException("Stored file not found on disk at path: " + filePath);
    }
    if (!Files.isReadable(filePath)) {
      throw new FileStorageException("File is not readable at path: " + filePath);
    }

    try {
      // Read and decompress into memory
      byte[] decompressedData;
      try (InputStream compressedStream = Files.newInputStream(filePath);
          GZIPInputStream decompressedStream = new GZIPInputStream(compressedStream);
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

        decompressedStream.transferTo(outputStream);
        decompressedData = outputStream.toByteArray();
      }

      return new ByteArrayResource(decompressedData);

    } catch (IOException e) {
      throw new FileStorageException("Failed to read or decompress file: " + metadata.getFilename(),
          e);
    }
  }

  /**
   * List the files that the user has access to.
   *
   * @param user user who wants to access the file
   * @return list of file metadata
   */
  public List<FileMetadataDto> listUserFiles(User user) {
    List<FileMetadata> fileMetadataList = fileMetadataRepository.findByOwnerOrSharedWith(user);
    return fileMetadataMapper.toDtoList(fileMetadataList);
  }

  /**
   * List all files stored in the system.
   *
   * @return list of file metadata
   */
  public List<FileMetadataDto> listAllFiles() {
    List<FileMetadata> fileMetadataList = fileMetadataRepository.findAll();
    return fileMetadataMapper.toDtoList(fileMetadataList);
  }

  /**
   * List all files with size that exceeds a given size in bytes.
   *
   * @param sizeInBytes size in bytes
   * @return list of file metadata
   */
  public List<FileMetadataDto> findFilesLargerThan(Long sizeInBytes) {
    List<FileMetadata> fileMetadataList = fileMetadataRepository.findLargerThan(sizeInBytes);
    return fileMetadataMapper.toDtoList(fileMetadataList);
  }

  private Long calculateUserTotalStorage(User user) {
    return fileMetadataRepository.sumSizeByOwner(user);
  }

  /**
   * Get the total storage size used for files.
   *
   * @return size in bytes
   */
  public Long calculateTotalStorageUsage() {
    return fileMetadataRepository.calculateTotalStorageUsage();
  }

  /**
   * Delete a file.
   *
   * @param fileId file id
   * @param owner  user who wants to delete the file
   */
  public void deleteFile(UUID fileId, User owner) {
    FileMetadata metadata = fileMetadataRepository.findByIdAndOwner(fileId, owner)
        .orElseThrow(() -> new RuntimeException("File not found"));
    try {
      Files.delete(rootLocation.resolve(metadata.getStoragePath()));
      fileMetadataRepository.delete(metadata);
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete file", e);
    }
  }

  /**
   * Share a file with another user.
   *
   * @param fileId   file id
   * @param username username of the user that we want to share the file with
   * @param readOnly the user should only be able to have read/download access to the file
   * @param owner    user who wants to share the file
   */
  public void shareFile(UUID fileId, String username, boolean readOnly, User owner) {
    FilePermission filePermission = new FilePermission();
    Optional<FileMetadata> fileMetadataOptional = fileMetadataRepository
        .findByIdAndOwner(fileId, owner);
    if (fileMetadataOptional.isEmpty()) {
      throw new RuntimeException("No file found with id: " + fileId + " and owner: " + owner);
    }

    FileMetadata fileMetadata = fileMetadataOptional.get();
    User user = userService.findByUsername(username);
    filePermission.setId(UUID.randomUUID());
    filePermission.setFileMetadata(fileMetadata);
    filePermission.setUser(user);
    FileAccessLevel fileAccessLevel = readOnly ? FileAccessLevel.VIEW : FileAccessLevel.OWNER;
    filePermission.setAccessLevel(fileAccessLevel);
    filePermissionRepository.save(filePermission);
  }
}