package com.mvasilakos.filestorage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.mapper.UserMapper;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.model.UserRole;
import com.mvasilakos.filestorage.repository.UserRepository;
import java.util.Arrays;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  private User testUser;
  private UserDto testUserDto;
  private UUID testUserId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    testUser = User.builder()
        .id(testUserId)
        .username("testUser")
        .password("encodedPassword")
        .email("test@example.com")
        .role(UserRole.USER)
        .build();

    testUserDto = UserDto.builder()
        .id(testUserId)
        .username("testUser")
        .email("test@example.com")
        .build();
  }

  @Test
  void registerUserValidDataShouldReturnUserDto() {
    // Arrange
    String username = "newUser";
    String password = "password123";
    String email = "new@example.com";
    String encodedPassword = "encodedPassword123";

    when(userRepository.existsByUsername(username)).thenReturn(false);
    when(userRepository.existsByEmail(email)).thenReturn(false);
    when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(userMapper.toDto(testUser)).thenReturn(testUserDto);

    // Act
    UserDto result = userService.registerUser(username, password, email);

    // Assert
    assertNotNull(result);
    assertEquals(testUserDto.id(), result.id());
    assertEquals(testUserDto.username(), result.username());
    assertEquals(testUserDto.email(), result.email());

    verify(userRepository).existsByUsername(username);
    verify(userRepository).existsByEmail(email);
    verify(passwordEncoder).encode(password);
    verify(userRepository).save(any(User.class));
    verify(userMapper).toDto(testUser);
  }

  @Test
  void registerUserWithAdminFlagShouldReturnAdminUserDto() {
    // Arrange
    String username = "adminUser";
    String email = "admin@example.com";
    String password = "password123";

    when(userRepository.existsByUsername(username)).thenReturn(false);
    when(userRepository.existsByEmail(email)).thenReturn(false);
    when(passwordEncoder.encode(password)).thenReturn("encodedPassword123");

    User adminUser = new User();
    adminUser.setRole(UserRole.ADMIN);
    when(userRepository.save(any(User.class))).thenReturn(adminUser);
    when(userMapper.toDto(adminUser)).thenReturn(testUserDto);

    // Act
    UserDto result = userService.registerUser(username, password, email, true);

    // Assert
    assertNotNull(result);
    verify(userRepository).save(argThat(user -> user.getRole() == UserRole.ADMIN));
  }

  @Test
  void registerUserWithAdminFlagFalseShouldReturnRegularUserDto() {
    // Arrange
    String username = "regularUser";
    String password = "password123";
    String email = "regular@example.com";
    boolean isAdmin = false;

    when(userRepository.existsByUsername(username)).thenReturn(false);
    when(userRepository.existsByEmail(email)).thenReturn(false);
    when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(userMapper.toDto(testUser)).thenReturn(testUserDto);

    // Act
    UserDto result = userService.registerUser(username, password, email, isAdmin);

    // Assert
    assertNotNull(result);
    verify(userRepository).save(argThat(user -> user.getRole() != UserRole.ADMIN));
  }

  @Test
  void registerUserExistingUsernameShouldThrowException() {
    // Arrange
    String username = "existingUser";
    String password = "password123";
    String email = "new@example.com";

    when(userRepository.existsByUsername(username)).thenReturn(true);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> userService.registerUser(username, password, email)
    );

    assertEquals("Username already exists", exception.getMessage());
    verify(userRepository).existsByUsername(username);
    verify(userRepository, never()).existsByEmail(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void registerUserExistingEmailShouldThrowException() {
    // Arrange
    String username = "newUser";
    String password = "password123";
    String email = "existing@example.com";

    when(userRepository.existsByUsername(username)).thenReturn(false);
    when(userRepository.existsByEmail(email)).thenReturn(true);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> userService.registerUser(username, password, email)
    );

    assertEquals("Email already exists", exception.getMessage());
    verify(userRepository).existsByUsername(username);
    verify(userRepository).existsByEmail(email);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void registerUserInvalidEmailShouldThrowException() {
    // Arrange
    String username = "newUser";
    String password = "password123";
    String email = "invalid-email";

    when(userRepository.existsByUsername(username)).thenReturn(false);
    when(userRepository.existsByEmail(email)).thenReturn(false);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> userService.registerUser(username, password, email)
    );

    assertEquals("Invalid email", exception.getMessage());
    verify(userRepository).existsByUsername(username);
    verify(userRepository).existsByEmail(email);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void getAllUsersShouldReturnUserDtoList() {
    // Arrange
    List<User> users = Arrays.asList(testUser, new User());
    List<UserDto> userDtos = Arrays.asList(testUserDto,
        new UserDto(
            UUID.randomUUID(),
            "testUser",
            "test@example.com"
        ));

    when(userRepository.findAll()).thenReturn(users);
    when(userMapper.toDtoList(users)).thenReturn(userDtos);

    // Act
    List<UserDto> result = userService.getAllUsers();

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    verify(userRepository).findAll();
    verify(userMapper).toDtoList(users);
  }

  @Test
  void searchUserValidSearchTermShouldReturnUserDtoList() {
    // Arrange
    String searchTerm = "test";
    List<User> users = Collections.singletonList(testUser);
    List<UserDto> userDtos = Collections.singletonList(testUserDto);

    when(userRepository.searchUser(searchTerm)).thenReturn(users);
    when(userMapper.toDtoList(users)).thenReturn(userDtos);

    // Act
    List<UserDto> result = userService.searchUser(searchTerm);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testUserDto.username(), result.get(0).username());
    verify(userRepository).searchUser(searchTerm);
    verify(userMapper).toDtoList(users);
  }

  @Test
  void findByIdExistingUserShouldReturnUser() {
    // Arrange
    when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

    // Act
    User result = userService.findById(testUserId);

    // Assert
    assertNotNull(result);
    assertEquals(testUser.getId(), result.getId());
    assertEquals(testUser.getUsername(), result.getUsername());
    verify(userRepository).findById(testUserId);
  }

  @Test
  void findByIdNonExistingUserShouldThrowException() {
    // Arrange
    UUID nonExistingId = UUID.randomUUID();
    when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> userService.findById(nonExistingId)
    );

    assertEquals("User not found", exception.getMessage());
    verify(userRepository).findById(nonExistingId);
  }

  @Test
  void findByUsernameExistingUserShouldReturnUser() {
    // Arrange
    String username = "testUser";
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

    // Act
    User result = userService.findByUsername(username);

    // Assert
    assertNotNull(result);
    assertEquals(testUser.getUsername(), result.getUsername());
    verify(userRepository).findByUsername(username);
  }

  @Test
  void findByUsernameNonExistingUserShouldThrowException() {
    // Arrange
    String username = "nonExisting";
    when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> userService.findByUsername(username)
    );

    assertEquals("User not found", exception.getMessage());
    verify(userRepository).findByUsername(username);
  }

  @Test
  void saveUserValidUserShouldCallRepository() {
    // Arrange
    when(userRepository.save(testUser)).thenReturn(testUser);

    // Act
    userService.saveUser(testUser);

    // Assert
    verify(userRepository).save(testUser);
  }

  @Test
  void createUserShouldGenerateIdAndEncodePassword() {
    // Arrange
    String username = "testUser";
    String password = "plainPassword";
    String email = "test@example.com";
    String encodedPassword = "encodedPassword";

    when(userRepository.existsByUsername(username)).thenReturn(false);
    when(userRepository.existsByEmail(email)).thenReturn(false);
    when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(userMapper.toDto(any(User.class))).thenReturn(testUserDto);

    // Act
    userService.registerUser(username, password, email);

    // Assert
    verify(userRepository).save(argThat(user ->
        user.getId() != null
            && user.getUsername().equals(username)
            && user.getPassword().equals(encodedPassword)
            && user.getEmail().equals(email)
    ));
  }
}