package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.exception.GenericException;
import com.mvasilakos.filestorage.exception.UserException;
import com.mvasilakos.filestorage.mapper.UserMapper;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.model.UserRole;
import com.mvasilakos.filestorage.repository.FileMetadataRepository;
import com.mvasilakos.filestorage.repository.UserRepository;
import com.mvasilakos.filestorage.validator.PasswordValidator;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * User service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  @Value("${file.storage.max-storage-per-user:1048576}")
  private long maxStoragePerUser;

  private final UserRepository userRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  /**
   * Delete user's own account.
   *
   * @param user the user who wants to delete their account
   */
  @Transactional
  public void deleteOwnAccount(User user) {
    try {
      userRepository.deleteById(user.getId());
    } catch (Exception e) {
      throw new GenericException(
          String.format("Couldn't delete user with id=%s and username=%s", user.getId(),
              user.getUsername()), e);
    }
  }

  /**
   * Delete user's account.
   *
   * @param userId the userId of the user to delete
   */
  @Transactional
  public void deleteUsersAccount(UUID userId) {
    try {
      userRepository.deleteById(userId);
      log.info("Successfully deleted user with ID {}", userId);
    } catch (Exception e) {
      throw new GenericException(
          String.format("Couldn't delete user with id=%s", userId),
          e);
    }
  }

  /**
   * Register a new user.
   *
   * @param username username
   * @param password password
   * @param email    email address
   * @return new user details
   */
  public UserDto registerUser(String username, String password, String email) {
    validateNewUser(username, password, email);
    User user = createUser(username, password, email);

    User registeredUser = userRepository.save(user);
    return userMapper.toDto(registeredUser);
  }

  /**
   * Register new user with the option to make them an admin.
   *
   * @param username username
   * @param password password
   * @param email    email address
   * @param isAdmin  true if the new user is an admin
   * @return new user details
   */
  public UserDto registerUser(String username, String password, String email, boolean isAdmin) {
    validateNewUser(username, password, email);
    User user = createUser(username, password, email);
    if (isAdmin) {
      user.setRole(UserRole.ADMIN);
    }
    User registeredUser = userRepository.save(user);
    return userMapper.toDto(registeredUser);
  }

  private void validateNewUser(String username, String password, String email) {
    if (userRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("Username already exists");
    } else if (userRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("Email already exists");
    } else if (!EmailValidator.getInstance().isValid(email)) {
      throw new IllegalArgumentException("Invalid email");
    } else if (!PasswordValidator.validatePassword(password)) {
      throw new IllegalArgumentException(
          "Password must contain at least one lowercase letter, one uppercase letter, one number, "
              + "one special character and be between 8 and 20 characters long.");
    }
  }

  private User createUser(String username, String password, String email) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setEmail(email);
    return user;
  }

  /**
   * List all the system's users.
   *
   * @return list of user details
   */
  public List<UserDto> getAllUsers() {
    List<User> users = userRepository.findAll();
    return userMapper.toDtoList(users);
  }

  /**
   * Count the total number of users.
   *
   * @return number of users
   */
  public long countAllUsers() {
    return userRepository.count();
  }

  /**
   * Search for a user based on their username or email, given a keyword.
   *
   * @param searchTerm search keyword
   * @return list of matching users
   */
  public List<UserDto> searchUser(String searchTerm) {
    List<User> users = userRepository.searchUser(searchTerm);
    return userMapper.toDtoList(users);
  }

  /**
   * Search for a user based on their username or email, given a keyword.
   *
   * @param searchTerm search keyword
   * @param page       the page number
   * @param size       the number of elements on the page
   * @return list of matching users
   */
  public Page<UserDto> searchUserPaginated(String searchTerm, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("username"));
    Page<User> usersPage = userRepository.searchUserPaginated(searchTerm, pageable);
    return usersPage.map(userMapper::toDto);
  }

  /**
   * Search for a user based on their username or email, given a keyword.
   *
   * @param id user id
   * @return user
   */
  public UserDto getAccountDetails(UUID id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new UserException(String.format("No user with id=%s found", id)));
    return userMapper.toDto(user);
  }

  /**
   * Find a user by their id.
   *
   * @param userId user id
   * @return user
   */
  protected User findById(UUID userId) {
    Optional<User> userOptional = userRepository.findById(userId);
    return userOptional.orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  /**
   * Find a user by their username.
   *
   * @param username username
   * @return user
   */
  public User findByUsername(String username) {
    Optional<User> userOptional = userRepository.findByUsername(username);
    return userOptional.orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  /**
   * Find user details by their username.
   *
   * @param username username
   * @return user details
   */
  public UserDetails findUserDetailsByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }


  /**
   * Persist the user.
   *
   * @param user user to be persisted
   */
  public void saveUser(User user) {
    userRepository.save(user);
  }

  /**
   * Calculate the user's used storage.
   *
   * @param user user
   * @return the bytes of storage used
   */
  public long getUserStorageUsed(User user) {
    return fileMetadataRepository.sumSizeByOwner(user);
  }

  /**
   * Get the maximum allowed storage size for a single user.
   *
   * @return the storage in bytes
   */
  public long getStorageLimitPerUser(User user) {
    // TODO: passing user variable to be able to get different storage limits per user
    return maxStoragePerUser;
  }

}