package com.mvasilakos.filestorage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.model.UserRole;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

  @Mock
  private FileService fileService;

  @Mock
  private UserService userService;

  @InjectMocks
  private AdminService adminService;

  private UUID testUserId;
  private User testUser;
  private UserDto testUserDto;
  private FileMetadataDto testFileMetadata;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(adminService, "adminUsername", "admin");
    testUserId = UUID.randomUUID();

    testUser = User.builder()
        .id(testUserId)
        .username("testUser")
        .email("test@example.com")
        .enabled(true)
        .role(UserRole.USER)
        .build();

    testUserDto = UserDto.builder()
        .id(testUserId)
        .username("testAdmin")
        .email("admin@example.com")
        .build();

    testFileMetadata = FileMetadataDto.builder()
        .id(UUID.randomUUID())
        .filename("largeFile.txt")
        .size(1000000L)
        .build();
  }

  @Test
  void registerAdminShouldCallUserServiceWithAdminFlag() {
    // Given
    String username = "adminUser";
    String password = "password123";
    String email = "admin@example.com";

    when(userService.registerUser(username, password, email, true))
        .thenReturn(testUserDto);

    // When
    UserDto result = adminService.registerAdmin(username, password, email);

    // Then
    assertNotNull(result);
    assertEquals(testUserDto, result);
    verify(userService).registerUser(username, password, email, true);
  }

  @Test
  void banUserShouldDisableUserAndSave() {
    // Given
    when(userService.findById(testUserId)).thenReturn(testUser);

    // When
    adminService.banUser(testUserId);

    // Then
    assertFalse(testUser.isEnabled());
    verify(userService).findById(testUserId);
    verify(userService).saveUser(testUser);
  }

  @Test
  void unbanUserShouldEnableUserAndSave() {
    // Given
    testUser.setEnabled(false);
    when(userService.findById(testUserId)).thenReturn(testUser);

    // When
    adminService.unbanUser(testUserId);

    // Then
    assertTrue(testUser.isEnabled());
    verify(userService).findById(testUserId);
    verify(userService).saveUser(testUser);
  }

  @Test
  void changeUserRoleWithValidRoleShouldUpdateUserRole() {
    // Given
    String newRole = "ADMIN";
    when(userService.findById(testUserId)).thenReturn(testUser);

    // Mock UserRole.fromString to return ADMIN role
    try (var mockedStatic = mockStatic(UserRole.class)) {
      mockedStatic.when(() -> UserRole.fromString(newRole))
          .thenReturn(Optional.of(UserRole.ADMIN));

      // When
      adminService.changeUserRole(testUserId, newRole);

      // Then
      assertEquals(UserRole.ADMIN, testUser.getRole());
      verify(userService).findById(testUserId);
      verify(userService).saveUser(testUser);
    }
  }

  @Test
  void changeUserRoleWithInvalidRoleShouldThrowException() {
    // Given
    String invalidRole = "INVALID_ROLE";

    try (var mockedStatic = mockStatic(UserRole.class)) {
      mockedStatic.when(() -> UserRole.fromString(invalidRole))
          .thenReturn(Optional.empty());

      // When & Then
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> adminService.changeUserRole(testUserId, invalidRole)
      );

      assertEquals("Role " + invalidRole + " not found", exception.getMessage());
      verify(userService, never()).findById(testUserId);
      verify(userService, never()).saveUser(any(User.class));
    }
  }

  @Test
  void getLargeFilesExceedingShouldReturnFilesFromFileService() {
    // Given
    long sizeInBytes = 500000L;
    List<FileMetadataDto> expectedFiles = Collections.singletonList(testFileMetadata);
    when(fileService.findFilesLargerThan(sizeInBytes)).thenReturn(expectedFiles);

    // When
    List<FileMetadataDto> result = adminService.getLargeFilesExceeding(sizeInBytes);

    // Then
    assertNotNull(result);
    assertEquals(expectedFiles, result);
    assertEquals(1, result.size());
    verify(fileService).findFilesLargerThan(sizeInBytes);
  }

  @Test
  void getLargeFilesExceedingWithNoLargeFilesShouldReturnEmptyList() {
    // Given
    long sizeInBytes = 2000000L;
    when(fileService.findFilesLargerThan(sizeInBytes)).thenReturn(List.of());

    // When
    List<FileMetadataDto> result = adminService.getLargeFilesExceeding(sizeInBytes);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(fileService).findFilesLargerThan(sizeInBytes);
  }

  @Test
  void getTotalStorageUsedShouldReturnStorageFromFileService() {
    // Given
    Long expectedStorage = 1024000L;
    when(fileService.calculateTotalStorageUsage()).thenReturn(expectedStorage);

    // When
    Long result = adminService.getTotalStorageUsed();

    // Then
    assertEquals(expectedStorage, result);
    verify(fileService).calculateTotalStorageUsage();
  }

  @Test
  void getTotalStorageUsedWhenNoStorageShouldReturnZero() {
    // Given
    when(fileService.calculateTotalStorageUsage()).thenReturn(0L);

    // When
    Long result = adminService.getTotalStorageUsed();

    // Then
    assertEquals(0L, result);
    verify(fileService).calculateTotalStorageUsage();
  }

  @Test
  void banUserWhenUserNotFoundShouldPropagateException() {
    // Given
    UUID nonExistentUserId = UUID.randomUUID();
    when(userService.findById(nonExistentUserId))
        .thenThrow(new RuntimeException("User not found"));

    // When & Then
    assertThrows(RuntimeException.class,
        () -> adminService.banUser(nonExistentUserId));

    verify(userService).findById(nonExistentUserId);
    verify(userService, never()).saveUser(any(User.class));
  }

  @Test
  void unbanUserWhenUserNotFoundShouldPropagateException() {
    // Given
    UUID nonExistentUserId = UUID.randomUUID();
    when(userService.findById(nonExistentUserId))
        .thenThrow(new RuntimeException("User not found"));

    // When & Then
    assertThrows(RuntimeException.class,
        () -> adminService.unbanUser(nonExistentUserId));

    verify(userService).findById(nonExistentUserId);
    verify(userService, never()).saveUser(any(User.class));
  }

  @Test
  void changeUserRoleWhenUserNotFoundShouldPropagateException() {
    // Given
    UUID nonExistentUserId = UUID.randomUUID();
    String newRole = "ADMIN";
    when(userService.findById(nonExistentUserId))
        .thenThrow(new RuntimeException("User not found"));

    // When & Then
    assertThrows(RuntimeException.class,
        () -> adminService.changeUserRole(nonExistentUserId, newRole));

    verify(userService).findById(nonExistentUserId);
    verify(userService, never()).saveUser(any(User.class));
  }
}