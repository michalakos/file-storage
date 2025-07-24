package com.mvasilakos.filestorage.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.exception.FileStorageException;
import com.mvasilakos.filestorage.mapper.FileMetadataMapper;
import com.mvasilakos.filestorage.model.FileAccessLevel;
import com.mvasilakos.filestorage.model.FileMetadata;
import com.mvasilakos.filestorage.model.FilePermission;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.repository.FileMetadataRepository;
import com.mvasilakos.filestorage.repository.FilePermissionRepository;
import com.mvasilakos.filestorage.service.FileStorageService.StoredFileData;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;


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
  private FileStorageService fileStorageService;

  @Mock
  private FileEncryptionService fileEncryptionService;

  @Mock
  private FileCompressionService fileCompressionService;

  @InjectMocks
  private FileService fileService;

  private User testUser;
  private User ownerUser;
  private FileMetadata testFileMetadata;
  private FileMetadataDto testFileMetadataDto;
  private UUID testFileId;


  @BeforeEach
  void setUp() {

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

    ReflectionTestUtils.setField(fileService, "maxStoragePerUser", 10485760L);
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
  void downloadFileAsResourceWhenFileExistsShouldReturnResource() {
    when(fileMetadataRepository.findByIdAndOwnerOrSharedWith(testFileId, testUser))
        .thenReturn(Optional.of(testFileMetadata));

    StoredFileData dummyStoredData = new StoredFileData(
        "dummyPath".getBytes(), "dummyData".getBytes());
    when(fileStorageService.readEncryptedFile(any())).thenReturn(dummyStoredData);

    when(fileCompressionService.decompress(any())).thenReturn("decompressedData".getBytes());
    when(fileEncryptionService.decrypt(any(), any())).thenReturn("file content".getBytes());

    // When
    Resource result = fileService.downloadFile(testFileId, testUser);

    // Then
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.isReadable());
    verify(fileMetadataRepository).findByIdAndOwnerOrSharedWith(testFileId, testUser);
    verify(fileStorageService).readEncryptedFile(any());
    verify(fileCompressionService).decompress(any());
    verify(fileEncryptionService).decrypt(any(), any());
  }

  @Test
  void downloadFileAsResourceWhenFileNotFoundShouldThrowException() {
    // Given
    when(fileMetadataRepository.findByIdAndOwnerOrSharedWith(testFileId, testUser))
        .thenReturn(Optional.empty());

    // When & Then
    assertThrows(RuntimeException.class, () -> fileService.downloadFile(testFileId, testUser));
  }

  @Test
  void downloadFileAsResourceWhenFileDoesNotExistOnDiskShouldThrowException() {
    // Given
    when(fileMetadataRepository.findByIdAndOwnerOrSharedWith(testFileId, testUser))
        .thenReturn(Optional.of(testFileMetadata));

    // When & Then
    assertThrows(RuntimeException.class, () -> fileService.downloadFile(testFileId, testUser));
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
  void deleteFileWhenExceptionOccursShouldThrowException() {
    // Given
    // File doesn't exist on disk, will cause IOException
    when(fileMetadataRepository.findByIdAndOwner(testFileId, ownerUser))
        .thenReturn(Optional.of(testFileMetadata));
    doThrow(new FileStorageException("")).when(fileStorageService).deleteFile(any());

    // When & Then
    assertThrows(FileStorageException.class, () -> fileService.deleteFile(testFileId, ownerUser));

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