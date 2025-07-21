package com.mvasilakos.filestorage.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.mapper.FileMetadataMapper;
import com.mvasilakos.filestorage.model.FileAccessLevel;
import com.mvasilakos.filestorage.model.FileMetadata;
import com.mvasilakos.filestorage.model.FilePermission;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.repository.FileMetadataRepository;
import com.mvasilakos.filestorage.repository.FilePermissionRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;


@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  @Mock
  private FileMetadataMapper fileMetadataMapper;

  @Mock
  private FileMetadataRepository fileMetadataRepository;

  @Mock
  private FilePermissionRepository filePermissionRepository;

  @Mock
  private UserService userService;

  @Mock
  private MultipartFile multipartFile;

  @TempDir
  Path tempDir;

  private FileService fileService;
  private User testUser;
  private User ownerUser;
  private FileMetadata testFileMetadata;
  private FileMetadataDto testFileMetadataDto;
  private UUID testFileId;


  @BeforeEach
  void setUp() {
    // Initialize FileService with temporary directory
    fileService = new FileService(
        fileMetadataRepository,
        fileMetadataMapper,
        filePermissionRepository,
        userService,
        tempDir.toString()
    );

    // Setup test data
    testFileId = UUID.randomUUID();

    testUser = User.builder()
        .id(UUID.randomUUID())
        .username("testUser")
        .email("test@example.com")
        .build();

    ownerUser = User.builder()
        .id(UUID.randomUUID())
        .username("owner")
        .email("owner@example.com")
        .build();

    testFileMetadata = FileMetadata.builder()
        .id(testFileId)
        .filename("test.txt")
        .contentType("text/plain")
        .size(1024L)
        .storagePath(testFileId + "_test.txt")
        .owner(ownerUser)
        .uploadDate(LocalDateTime.now())
        .build();

    testFileMetadataDto = FileMetadataDto.builder()
        .id(testFileId)
        .filename("test.txt")
        .contentType("text/plain")
        .size(1024L)
        .build();
  }

  @Test
  void constructorWhenInitStorageFailsShouldThrowRuntimeException() {
    // Given
    String invalidStoragePath = "/root/invalid/path/that/cannot/be/created";

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        new FileService(
            fileMetadataRepository,
            fileMetadataMapper,
            filePermissionRepository,
            userService,
            invalidStoragePath
        ));

    assertEquals("Could not initialize storage", exception.getMessage());
    assertInstanceOf(IOException.class, exception.getCause());
  }

  @Test
  void storeFileWithNullFilenameShouldHandleNullFilename() throws IOException {
    // Given
    String contentType = "text/plain";
    long size = 1024L;
    byte[] content = "test content".getBytes();

    when(multipartFile.getOriginalFilename()).thenReturn(null);
    when(multipartFile.getContentType()).thenReturn(contentType);
    when(multipartFile.getSize()).thenReturn(size);
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);
    when(fileMetadataMapper.toDto(testFileMetadata)).thenReturn(testFileMetadataDto);

    // When
    FileMetadataDto result = fileService.storeFile(multipartFile, ownerUser);

    // Then
    assertNotNull(result);
    ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(metadataCaptor.capture());
    FileMetadata savedMetadata = metadataCaptor.getValue();

    assertNull(savedMetadata.getFilename());
    assertEquals(contentType, savedMetadata.getContentType());
    assertEquals(size, savedMetadata.getSize());
    assertTrue(savedMetadata.getStoragePath().startsWith(
        savedMetadata.getId().toString() + "_null"));
  }

  @Test
  void storeFileWithNullContentTypeShouldHandleNullContentType() throws IOException {
    // Given
    String originalFilename = "test.txt";
    long size = 1024L;
    byte[] content = "test content".getBytes();

    when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
    when(multipartFile.getContentType()).thenReturn(null);
    when(multipartFile.getSize()).thenReturn(size);
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);
    when(fileMetadataMapper.toDto(testFileMetadata)).thenReturn(testFileMetadataDto);

    // When
    FileMetadataDto result = fileService.storeFile(multipartFile, ownerUser);

    // Then
    assertNotNull(result);
    ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(metadataCaptor.capture());
    FileMetadata savedMetadata = metadataCaptor.getValue();

    assertEquals(originalFilename, savedMetadata.getFilename());
    assertNull(savedMetadata.getContentType());
    assertEquals(size, savedMetadata.getSize());
  }

  @Test
  void storeFileWithZeroSizeShouldHandleZeroSizeFile() throws IOException {
    // Given
    String originalFilename = "empty.txt";
    String contentType = "text/plain";
    long size = 0L;
    byte[] content = new byte[0];

    when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
    when(multipartFile.getContentType()).thenReturn(contentType);
    when(multipartFile.getSize()).thenReturn(size);
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);
    when(fileMetadataMapper.toDto(testFileMetadata)).thenReturn(testFileMetadataDto);

    // When
    FileMetadataDto result = fileService.storeFile(multipartFile, ownerUser);

    // Then
    assertNotNull(result);
    ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(metadataCaptor.capture());
    FileMetadata savedMetadata = metadataCaptor.getValue();

    assertEquals(originalFilename, savedMetadata.getFilename());
    assertEquals(contentType, savedMetadata.getContentType());
    assertEquals(0L, savedMetadata.getSize());
  }

  @Test
  void storeFileWithLargeFileShouldHandleLargeSize() throws IOException {
    // Given
    String originalFilename = "largeFile.bin";
    String contentType = "application/octet-stream";
    long size = Long.MAX_VALUE;
    byte[] content = "large file content".getBytes();

    when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
    when(multipartFile.getContentType()).thenReturn(contentType);
    when(multipartFile.getSize()).thenReturn(size);
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);
    when(fileMetadataMapper.toDto(testFileMetadata)).thenReturn(testFileMetadataDto);

    // When
    FileMetadataDto result = fileService.storeFile(multipartFile, ownerUser);

    // Then
    assertNotNull(result);
    ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(metadataCaptor.capture());
    FileMetadata savedMetadata = metadataCaptor.getValue();

    assertEquals(originalFilename, savedMetadata.getFilename());
    assertEquals(contentType, savedMetadata.getContentType());
    assertEquals(Long.MAX_VALUE, savedMetadata.getSize());
  }

  @Test
  void storeFileWithBothNullFilenameAndContentTypeShouldHandleBothNull() throws IOException {
    // Given
    long size = 512L;
    byte[] content = "test content".getBytes();

    when(multipartFile.getOriginalFilename()).thenReturn(null);
    when(multipartFile.getContentType()).thenReturn(null);
    when(multipartFile.getSize()).thenReturn(size);
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);
    when(fileMetadataMapper.toDto(testFileMetadata)).thenReturn(testFileMetadataDto);

    // When
    FileMetadataDto result = fileService.storeFile(multipartFile, ownerUser);

    // Then
    assertNotNull(result);
    ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(metadataCaptor.capture());
    FileMetadata savedMetadata = metadataCaptor.getValue();

    assertNull(savedMetadata.getFilename());
    assertNull(savedMetadata.getContentType());
    assertEquals(size, savedMetadata.getSize());
    assertNotNull(savedMetadata.getId());
    assertNotNull(savedMetadata.getUploadDate());
    assertEquals(ownerUser, savedMetadata.getOwner());
  }

  @Test
  void storeFileWithEmptyFilenameShouldHandleEmptyFilename() throws IOException {
    // Given
    String originalFilename = "";
    String contentType = "text/plain";
    long size = 100L;
    byte[] content = "test".getBytes();

    when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
    when(multipartFile.getContentType()).thenReturn(contentType);
    when(multipartFile.getSize()).thenReturn(size);
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);
    when(fileMetadataMapper.toDto(testFileMetadata)).thenReturn(testFileMetadataDto);

    // When
    FileMetadataDto result = fileService.storeFile(multipartFile, ownerUser);

    // Then
    assertNotNull(result);
    ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(metadataCaptor.capture());
    FileMetadata savedMetadata = metadataCaptor.getValue();

    assertEquals("", savedMetadata.getFilename());
    assertEquals(contentType, savedMetadata.getContentType());
    assertEquals(size, savedMetadata.getSize());
    assertTrue(savedMetadata.getStoragePath().endsWith("_"));
  }

  @Test
  void storeFileWithSpecialCharactersInFilenameShouldHandleSpecialCharacters() throws IOException {
    // Given
    String originalFilename = "file with spaces & special chars!@#$%.txt";
    String contentType = "text/plain";
    long size = 256L;
    byte[] content = "special content".getBytes();

    when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
    when(multipartFile.getContentType()).thenReturn(contentType);
    when(multipartFile.getSize()).thenReturn(size);
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);
    when(fileMetadataMapper.toDto(testFileMetadata)).thenReturn(testFileMetadataDto);

    // When
    FileMetadataDto result = fileService.storeFile(multipartFile, ownerUser);

    // Then
    assertNotNull(result);
    ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(metadataCaptor.capture());
    FileMetadata savedMetadata = metadataCaptor.getValue();

    assertEquals(originalFilename, savedMetadata.getFilename());
    assertEquals(contentType, savedMetadata.getContentType());
    assertEquals(size, savedMetadata.getSize());
    assertTrue(savedMetadata.getStoragePath().contains(originalFilename));
  }

  @Test
  void storeFileShouldStoreFileAndReturnMetadata() throws IOException {
    // Given
    String originalFilename = "test.txt";
    String contentType = "text/plain";
    long size = 1024L;
    byte[] content = "test content".getBytes();

    when(multipartFile.getOriginalFilename()).thenReturn(originalFilename);
    when(multipartFile.getContentType()).thenReturn(contentType);
    when(multipartFile.getSize()).thenReturn(size);
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);
    when(fileMetadataMapper.toDto(testFileMetadata)).thenReturn(testFileMetadataDto);

    // When
    FileMetadataDto result = fileService.storeFile(multipartFile, ownerUser);

    // Then
    assertNotNull(result);
    assertEquals(testFileMetadataDto, result);

    ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(metadataCaptor.capture());
    FileMetadata savedMetadata = metadataCaptor.getValue();

    assertEquals(originalFilename, savedMetadata.getFilename());
    assertEquals(contentType, savedMetadata.getContentType());
    assertEquals(size, savedMetadata.getSize());
    assertEquals(ownerUser, savedMetadata.getOwner());
    assertNotNull(savedMetadata.getId());
    assertNotNull(savedMetadata.getUploadDate());
    assertTrue(savedMetadata.getStoragePath().contains(originalFilename));
  }

  @Test
  void storeFileWhenExceptionOccursShouldThrowRuntimeException() throws IOException {
    // Given
    when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
    when(multipartFile.getContentType()).thenReturn("text/plain");
    when(multipartFile.getSize()).thenReturn(1024L);
    when(multipartFile.getInputStream()).thenThrow(new IOException("IO Error"));

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> fileService.storeFile(multipartFile, ownerUser));

    assertEquals("Failed to store file", exception.getMessage());
    verify(fileMetadataRepository, never()).save(any());
  }

  @Test
  void renameFileShouldChangeFileNameAndReturnMetadata() {
    // Given
    String newFilename = "newTest.txt";
    FileMetadata renamedFileMetadata = testFileMetadata
        .toBuilder()
        .filename(newFilename)
        .build();
    FileMetadataDto renamedFileMetadataDto = testFileMetadataDto
        .toBuilder()
        .filename(newFilename)
        .build();

    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.ofNullable(testFileMetadata));
    when(fileMetadataRepository.save(renamedFileMetadata)).thenReturn(renamedFileMetadata);
    when(fileMetadataMapper.toDto(renamedFileMetadata)).thenReturn(renamedFileMetadataDto);

    // When
    FileMetadataDto result = fileService.renameFile(
        testFileMetadata.getId(), newFilename, ownerUser);

    // Then
    assertNotNull(result);
    assertEquals(renamedFileMetadataDto, result);

    ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(metadataCaptor.capture());
    FileMetadata savedMetadata = metadataCaptor.getValue();

    assertEquals(newFilename, savedMetadata.getFilename());
    assertEquals(renamedFileMetadata.getContentType(), savedMetadata.getContentType());
    assertEquals(renamedFileMetadata.getSize(), savedMetadata.getSize());
    assertEquals(ownerUser, savedMetadata.getOwner());
    assertNotNull(savedMetadata.getId());
    assertNotNull(savedMetadata.getUploadDate());
  }

  @Test
  void renameFileShouldThrowIfNoFileIsFoundForThisOwner() {
    // Given
    String newFilename = "newTest.txt";
    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> fileService.renameFile(testFileMetadata.getId(), newFilename, ownerUser));

    assertEquals("File not found", exception.getMessage());
    verify(fileMetadataRepository, never()).save(any());
    verify(fileMetadataMapper, never()).toDto(any());
  }

  @Test
  void getFileMetadataWhenFileExistsShouldReturnMetadata() {
    // Given
    when(fileMetadataRepository.findByIdAndOwnerOrSharedWith(testFileId, testUser))
        .thenReturn(Optional.of(testFileMetadata));
    when(fileMetadataMapper.toDto(testFileMetadata)).thenReturn(testFileMetadataDto);

    // When
    FileMetadataDto result = fileService.getFileMetadata(testFileId, testUser);

    // Then
    assertNotNull(result);
    assertEquals(testFileMetadataDto, result);
    verify(fileMetadataRepository).findByIdAndOwnerOrSharedWith(testFileId, testUser);
    verify(fileMetadataMapper).toDto(testFileMetadata);
  }

  @Test
  void getFileMetadataWhenFileNotFoundShouldThrowException() {
    // Given
    when(fileMetadataRepository.findByIdAndOwnerOrSharedWith(testFileId, testUser))
        .thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> fileService.getFileMetadata(testFileId, testUser));

    assertEquals("File not found", exception.getMessage());
    verify(fileMetadataMapper, never()).toDto(any());
  }

  @Test
  void loadFileAsResourceWhenFileExistsShouldReturnResource() throws IOException {
    // Given
    // Create a temporary file
    Path testFile = tempDir.resolve(testFileMetadata.getStoragePath());
    java.nio.file.Files.write(testFile, "test content".getBytes());

    when(fileMetadataRepository.findByIdAndOwnerOrSharedWith(testFileId, testUser))
        .thenReturn(Optional.of(testFileMetadata));

    // When
    Resource result = fileService.loadFileAsResource(testFileId, testUser);

    // Then
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.isReadable());
    verify(fileMetadataRepository).findByIdAndOwnerOrSharedWith(testFileId, testUser);
  }

  @Test
  void loadFileAsResourceWhenFileNotFoundShouldThrowException() {
    // Given
    when(fileMetadataRepository.findByIdAndOwnerOrSharedWith(testFileId, testUser))
        .thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> fileService.loadFileAsResource(testFileId, testUser));

    assertEquals("File not found", exception.getMessage());
  }

  @Test
  void loadFileAsResourceWhenFileDoesNotExistOnDiskShouldThrowException() {
    // Given
    when(fileMetadataRepository.findByIdAndOwnerOrSharedWith(testFileId, testUser))
        .thenReturn(Optional.of(testFileMetadata));

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> fileService.loadFileAsResource(testFileId, testUser));

    assertEquals("Could not read file", exception.getMessage());
  }

  @Test
  void listUserFilesShouldReturnUserAccessibleFiles() {
    // Given
    List<FileMetadata> metadataList = Collections.singletonList(testFileMetadata);
    List<FileMetadataDto> dtoList = Collections.singletonList(testFileMetadataDto);

    when(fileMetadataRepository.findByOwnerOrSharedWith(testUser)).thenReturn(metadataList);
    when(fileMetadataMapper.toDtoList(metadataList)).thenReturn(dtoList);

    // When
    List<FileMetadataDto> result = fileService.listUserFiles(testUser);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(dtoList, result);
    verify(fileMetadataRepository).findByOwnerOrSharedWith(testUser);
    verify(fileMetadataMapper).toDtoList(metadataList);
  }

  @Test
  void listAllFilesShouldReturnAllFiles() {
    // Given
    List<FileMetadata> metadataList = Collections.singletonList(testFileMetadata);
    List<FileMetadataDto> dtoList = Collections.singletonList(testFileMetadataDto);

    when(fileMetadataRepository.findAll()).thenReturn(metadataList);
    when(fileMetadataMapper.toDtoList(metadataList)).thenReturn(dtoList);

    // When
    List<FileMetadataDto> result = fileService.listAllFiles();

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(dtoList, result);
    verify(fileMetadataRepository).findAll();
    verify(fileMetadataMapper).toDtoList(metadataList);
  }

  @Test
  void findFilesLargerThanShouldReturnLargeFiles() {
    // Given
    Long sizeInBytes = 500L;
    List<FileMetadata> metadataList = Collections.singletonList(testFileMetadata);
    List<FileMetadataDto> dtoList = Collections.singletonList(testFileMetadataDto);

    when(fileMetadataRepository.findLargerThan(sizeInBytes)).thenReturn(metadataList);
    when(fileMetadataMapper.toDtoList(metadataList)).thenReturn(dtoList);

    // When
    List<FileMetadataDto> result = fileService.findFilesLargerThan(sizeInBytes);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(dtoList, result);
    verify(fileMetadataRepository).findLargerThan(sizeInBytes);
    verify(fileMetadataMapper).toDtoList(metadataList);
  }

  @Test
  void calculateTotalStorageUsageShouldSumAllFileSizes() {
    // Given
    FileMetadataDto file1 = FileMetadataDto.builder()
        .size(1024L)
        .build();
    FileMetadataDto file2 = FileMetadataDto.builder()
        .size(2048L)
        .build();

    List<FileMetadata> metadataList = Arrays.asList(testFileMetadata, new FileMetadata());
    List<FileMetadataDto> dtoList = Arrays.asList(file1, file2);

    when(fileMetadataRepository.findAll()).thenReturn(metadataList);
    when(fileMetadataMapper.toDtoList(metadataList)).thenReturn(dtoList);

    // When
    Long result = fileService.calculateTotalStorageUsage();

    // Then
    assertEquals(3072L, result);
    verify(fileMetadataRepository).findAll();
    verify(fileMetadataMapper).toDtoList(metadataList);
  }

  @Test
  void calculateTotalStorageUsageWhenNoFilesShouldReturnZero() {
    // Given
    when(fileMetadataRepository.findAll()).thenReturn(List.of());
    when(fileMetadataMapper.toDtoList(any())).thenReturn(List.of());

    // When
    Long result = fileService.calculateTotalStorageUsage();

    // Then
    assertEquals(0L, result);
  }

  @Test
  void deleteFileWhenFileExistsShouldDeleteFileAndMetadata() throws IOException {
    // Given
    // Create a temporary file
    Path testFile = tempDir.resolve(testFileMetadata.getStoragePath());
    java.nio.file.Files.write(testFile, "test content".getBytes());

    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.of(testFileMetadata));

    // When
    fileService.deleteFile(testFileId, ownerUser);

    // Then
    assertFalse(java.nio.file.Files.exists(testFile));
    verify(fileMetadataRepository).findByIdAndOwner(testFileId, ownerUser);
    verify(fileMetadataRepository).delete(testFileMetadata);
  }

  @Test
  void deleteFileWhenFileNotFoundShouldThrowException() {
    // Given
    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> fileService.deleteFile(testFileId, ownerUser));

    assertEquals("File not found", exception.getMessage());
    verify(fileMetadataRepository, never()).delete(any());
  }

  @Test
  void deleteFileWhenExceptionOccursShouldThrowRuntimeException() {
    // Given
    // File doesn't exist on disk, will cause IOException
    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.of(testFileMetadata));

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> fileService.deleteFile(testFileId, ownerUser));

    assertEquals("Failed to delete file", exception.getMessage());
    verify(fileMetadataRepository, never()).delete(any());
  }

  @Test
  void shareFileWithValidDataShouldCreatePermission() {
    // Given
    String username = "shareUser";
    User shareUser = new User();
    shareUser.setUsername(username);
    boolean readOnly = true;

    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.of(testFileMetadata));
    when(userService.findByUsername(username)).thenReturn(shareUser);

    // When
    assertDoesNotThrow(() -> fileService.shareFile(testFileId, username, readOnly, ownerUser));

    // Then
    ArgumentCaptor<FilePermission> permissionCaptor = ArgumentCaptor.forClass(FilePermission.class);
    verify(filePermissionRepository).save(permissionCaptor.capture());
    FilePermission savedPermission = permissionCaptor.getValue();

    assertNotNull(savedPermission.getId());
    assertEquals(testFileMetadata, savedPermission.getFileMetadata());
    assertEquals(shareUser, savedPermission.getUser());
    assertEquals(FileAccessLevel.VIEW, savedPermission.getAccessLevel());
  }

  @Test
  void shareFileWithWriteAccessShouldSetOwnerAccessLevel() {
    // Given
    String username = "shareUser";
    User shareUser = new User();
    shareUser.setUsername(username);
    boolean readOnly = false;

    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.of(testFileMetadata));
    when(userService.findByUsername(username)).thenReturn(shareUser);

    // When
    assertDoesNotThrow(() -> fileService.shareFile(testFileId, username, readOnly, ownerUser));

    // Then
    ArgumentCaptor<FilePermission> permissionCaptor = ArgumentCaptor.forClass(FilePermission.class);
    verify(filePermissionRepository).save(permissionCaptor.capture());
    FilePermission savedPermission = permissionCaptor.getValue();

    assertEquals(FileAccessLevel.OWNER, savedPermission.getAccessLevel());
  }

  @Test
  void shareFileWhenFileNotFoundShouldThrowException() {
    // Given
    String username = "shareUser";
    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> fileService.shareFile(testFileId, username, true, ownerUser));

    assertEquals("No file found with id: " + testFileId + " and owner: " + ownerUser,
        exception.getMessage());
    verify(userService, never()).findByUsername(anyString());
    verify(filePermissionRepository, never()).save(any());
  }

  @Test
  void shareFileWhenUserServiceThrowsExceptionShouldPropagateException() {
    // Given
    String username = "nonExistentUser";
    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.of(testFileMetadata));
    when(userService.findByUsername(username))
        .thenThrow(new RuntimeException("User not found"));

    // When & Then
    assertThrows(RuntimeException.class,
        () -> fileService.shareFile(testFileId, username, true, ownerUser));

    verify(filePermissionRepository, never()).save(any());
  }

}