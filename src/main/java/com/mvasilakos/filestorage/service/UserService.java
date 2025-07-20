package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.mapper.UserMapper;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.model.UserRole;
import com.mvasilakos.filestorage.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * User service.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;


  /**
   * Register a new user.
   *
   * @param username username
   * @param password password
   * @param email email address
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
   * @param email email address
   * @param isAdmin true if the new user is an admin
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

  // FIXME: add password and email validation
  private void validateNewUser(String username, String password, String email) {
    if (userRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("Username already exists");
    } else if (userRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("Email already exists");
    } else if (!EmailValidator.getInstance().isValid(email)) {
      throw new IllegalArgumentException("Invalid email");
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
   * @param userId user id
   * @return user
   */
  protected User findByUsername(String userId) {
    Optional<User> userOptional = userRepository.findByUsername(userId);
    return userOptional.orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  /**
   * Persist the user.
   *
   * @param user user to be persisted
   */
  public void saveUser(User user) {
    userRepository.save(user);
  }

}