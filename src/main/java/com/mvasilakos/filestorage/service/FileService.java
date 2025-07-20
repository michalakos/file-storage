package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.mapper.FileMetadataMapper;
import com.mvasilakos.filestorage.model.FileAccessLevel;
import com.mvasilakos.filestorage.model.FileMetadata;
import com.mvasilakos.filestorage.model.FilePermission;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.repository.FileMetadataRepository;
import com.mvasilakos.filestorage.repository.FilePermissionRepository;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


/**
 * File service.
 */
@Service
public class FileService {

  private final FileMetadataMapper fileMetadataMapper;
  private final FileMetadataRepository fileMetadataRepository;
  private final FilePermissionRepository filePermissionRepository;
  private final UserService userService;
  private final Path rootLocation;

  /**
   * Constructor.
   *
   * @param fileMetadataRepository fileMetadataRepository
   * @param fileMetadataMapper fileMetadataMapper
   * @param filePermissionRepository filePermissionRepository
   * @param userService userService
   * @param storagePath storagePath
   */
  public FileService(
      FileMetadataRepository fileMetadataRepository,
      FileMetadataMapper fileMetadataMapper,
      FilePermissionRepository filePermissionRepository,
      UserService userService,
      @Value("${storage.location}") String storagePath) {
    this.fileMetadataMapper = fileMetadataMapper;
    this.fileMetadataRepository = fileMetadataRepository;
    this.filePermissionRepository = filePermissionRepository;
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
   * @param file file
   * @param owner the user who stored the file
   * @return file metadata
   */
  public FileMetadataDto storeFile(MultipartFile file, User owner) {
    try {
      FileMetadata metadata = createFileMetadata(file, owner);
      String location = metadata.getStoragePath();
      Files.copy(file.getInputStream(), rootLocation.resolve(location));
      FileMetadata fileMetadata = fileMetadataRepository.save(metadata);
      return fileMetadataMapper.toDto(fileMetadata);
    } catch (IOException e) {
      throw new RuntimeException("Failed to store file", e);
    }
  }

  private FileMetadata createFileMetadata(MultipartFile file, User owner) {
    UUID id = UUID.randomUUID();
    String location = id + "_" + file.getOriginalFilename();

    FileMetadata metadata = new FileMetadata();
    metadata.setId(id);
    metadata.setFilename(file.getOriginalFilename());
    metadata.setContentType(file.getContentType());
    metadata.setSize(file.getSize());
    metadata.setUploadDate(LocalDateTime.now());
    metadata.setStoragePath(location);
    metadata.setOwner(owner);
    return metadata;
  }

  /**
   * Get metadata for a file.
   *
   * @param fileId file id
   * @param user user who wants to access the file
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
   * @param user user who wants to access the file
   * @return file content
   */
  public Resource loadFileAsResource(UUID fileId, User user) {
    FileMetadata metadata = fileMetadataRepository.findByIdAndOwnerOrSharedWith(fileId, user)
        .orElseThrow(() -> new RuntimeException("File not found"));

    Path filePath = rootLocation.resolve(metadata.getStoragePath());
    try {
      Resource resource = new UrlResource(filePath.toUri());
      if (resource.exists() || resource.isReadable()) {
        return resource;
      } else {
        throw new RuntimeException("Could not read file");
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException("Could not read file", e);
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

  /**
   * Get the total storage size used for files.
   *
   * @return size in bytes
   */
  public Long calculateTotalStorageUsage() {
    return listAllFiles().stream().mapToLong(FileMetadataDto::size).sum();
  }

  /**
   * Delete a file.
   *
   * @param fileId file id
   * @param owner user who wants to delete the file
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
   * @param fileId file id
   * @param username username of the user that we want to share the file with
   * @param readOnly the user should only be able to have read/download access to the file
   * @param owner user who wants to share the file
   * @return true if the operation was successful
   */
  public Boolean shareFile(UUID fileId, String username, boolean readOnly, User owner) {
    FilePermission filePermission = new FilePermission();
    Optional<FileMetadata> fileMetadataOptional = fileMetadataRepository
        .findByIdAndOwner(fileId, owner);
    if (fileMetadataOptional.isEmpty()) {
      return false;
    }

    FileMetadata fileMetadata = fileMetadataOptional.get();
    User user = userService.findByUsername(username);
    filePermission.setId(UUID.randomUUID());
    filePermission.setFileMetadata(fileMetadata);
    filePermission.setUser(user);
    FileAccessLevel fileAccessLevel = readOnly ? FileAccessLevel.VIEW : FileAccessLevel.OWNER;
    filePermission.setAccessLevel(fileAccessLevel);
    filePermissionRepository.save(filePermission);
    return true;
  }
}