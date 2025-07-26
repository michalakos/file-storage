package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.exception.FileStorageException;
import com.mvasilakos.filestorage.mapper.FileMetadataMapper;
import com.mvasilakos.filestorage.model.FileAccessLevel;
import com.mvasilakos.filestorage.model.FileMetadata;
import com.mvasilakos.filestorage.model.FilePermission;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.repository.FileMetadataRepository;
import com.mvasilakos.filestorage.repository.FilePermissionRepository;
import com.mvasilakos.filestorage.validator.FileValidator;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


/**
 * File service.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

  @Value("${file.storage.max-storage-per-user:1048576}")
  private long maxStoragePerUser;

  private final FileMetadataMapper fileMetadataMapper;
  private final FileMetadataRepository fileMetadataRepository;
  private final FilePermissionRepository filePermissionRepository;
  private final FileValidator fileValidator;
  private final UserService userService;
  private final FileEncryptionService fileEncryptionService;
  private final FileCompressionService fileCompressionService;
  private final FileStorageService fileStorageService;


  /**
   * Store file.
   *
   * @param file  file to store
   * @param owner the user who is storing the file
   * @return file metadata DTO
   * @throws FileStorageException if storage fails or limits are exceeded
   */
  public FileMetadataDto uploadFile(MultipartFile file, User owner) {
    fileValidator.validateFile(file);

    long userStorageUsed = calculateUserTotalStorage(owner);
    long originalFileSize = file.getSize();
    validateStorageLimit(userStorageUsed, originalFileSize);

    FileMetadata metadata = createFileMetadata(file, owner);

    try {
      FileEncryptionService.EncryptionResult encryptionResult =
          fileEncryptionService.encryptStream(file.getInputStream());

      byte[] compressedData = fileCompressionService.compress(encryptionResult.encryptedData());

      fileStorageService.storeEncryptedFile(
          metadata.getStoragePath(),
          encryptionResult.iv(),
          compressedData
      );

      long finalStoredFileSize = fileStorageService.getFileSize(metadata.getStoragePath());
      metadata.setSize(finalStoredFileSize);
      metadata.setOriginalFileSize(originalFileSize);

      FileMetadata savedMetadata = fileMetadataRepository.save(metadata);
      return fileMetadataMapper.toDto(savedMetadata);

    } catch (IOException e) {
      cleanupAfterUploadFail(metadata);
      throw new FileStorageException(
          String.format("Failed to process file: \"%s\"", file.getOriginalFilename()), e);
    } catch (Exception e) {
      cleanupAfterUploadFail(metadata);
      throw new FileStorageException(
          String.format("Failed to encrypt/store file: \"%s\"", file.getOriginalFilename()), e);
    }
  }

  private void validateStorageLimit(long userStorageUsed, long fileSize) {
    if (userStorageUsed + fileSize > maxStoragePerUser) {
      long availableBytes = maxStoragePerUser - userStorageUsed;

      throw new FileStorageException(
          String.format("User storage limit exceeded. Used: %s, Available: %s, File size: %s",
              formatBytes(userStorageUsed),
              formatBytes(availableBytes),
              formatBytes(fileSize))
      );
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

  private void cleanupAfterUploadFail(FileMetadata metadata) {
    try {
      fileStorageService.deleteFile(metadata.getStoragePath());
    } catch (Exception e) {
      log.warn("Failed to cleanup file after upload failure: {}", metadata.getStoragePath(), e);
    }
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
        .orElseThrow(() -> new FileStorageException("File not found"));
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
        .orElseThrow(() -> new FileStorageException("File not found"));
    return fileMetadataMapper.toDto(metadata);
  }

  /**
   * Get file data.
   *
   * @param fileId file id
   * @param user   user who wants to access the file
   * @return file content
   */
  public Resource downloadFile(UUID fileId, User user) {
    FileMetadata metadata = fileMetadataRepository.findByIdAndOwnerOrSharedWith(fileId, user)
        .orElseThrow(() -> new FileStorageException("File not found for ID: " + fileId));

    try {
      FileStorageService.StoredFileData storedData = fileStorageService.readEncryptedFile(
          metadata.getStoragePath());

      byte[] encryptedData = fileCompressionService.decompress(
          storedData.compressedEncryptedData());

      byte[] originalData = fileEncryptionService.decrypt(encryptedData, storedData.iv());

      return new ByteArrayResource(originalData);

    } catch (Exception e) {
      throw new FileStorageException(
          String.format("Failed to process file: \"%s\"", metadata.getFilename()), e);
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
  @Transactional
  public void deleteFile(UUID fileId, User owner) {
    FileMetadata metadata = fileMetadataRepository.findByIdAndOwner(fileId, owner)
        .orElseThrow(() -> new FileStorageException("File not found"));
    fileStorageService.deleteFile(metadata.getStoragePath());
    fileMetadataRepository.delete(metadata);
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
      throw new FileStorageException("No file found with id: " + fileId + " and owner: " + owner);
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