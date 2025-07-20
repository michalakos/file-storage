package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.model.UserRole;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * Admin service.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

  private final FileService fileService;
  private final UserService userService;


  /**
   * Register a new admin.
   *
   * @param username username
   * @param password password
   * @param email email address
   * @return user details
   */
  public UserDto registerAdmin(String username, String password, String email) {
    return userService.registerUser(username, password, email, true);
  }

  /**
   * Ban user.
   *
   * @param userId id of the user to ban
   */
  @Transactional
  public void banUser(UUID userId) {
    User user = userService.findById(userId);
    user.setEnabled(false);
    userService.saveUser(user);
  }

  /**
   * Unban user.
   *
   * @param userId id of the user to unban.
   */
  @Transactional
  public void unbanUser(UUID userId) {
    User user = userService.findById(userId);
    user.setEnabled(true);
    userService.saveUser(user);
  }

  /**
   * Change user role (e.g. from user to admin).
   *
   * @param userId id of the user
   * @param newRole the new role of the user
   */
  @Transactional
  public void changeUserRole(UUID userId, String newRole) {
    Optional<UserRole> userRoleOptional = UserRole.fromString(newRole);
    UserRole role = userRoleOptional.orElseThrow(
        () -> new IllegalArgumentException("Role " + newRole + " not found"));
    User user = userService.findById(userId);
    user.setRole(role);
    userService.saveUser(user);
  }

  /**
   * List all files with a size larger than the given size in bytes.
   *
   * @param sizeInBytes size in bytes
   * @return list of file metadata
   */
  public List<FileMetadataDto> getLargeFilesExceeding(long sizeInBytes) {
    return fileService.findFilesLargerThan(sizeInBytes);
  }

  /**
   * Calculate and return the total size of used storage in bytes.
   *
   * @return size in bytes
   */
  public Long getTotalStorageUsed() {
    return fileService.calculateTotalStorageUsage();
  }

  // TODO: implement method
  /**
   * Return the amount of active sessions.
   *
   * @return number of active sessions.
   */
  public Integer getActiveSessionCount() {
    // Query Spring Security or session management for active sessions
    return 0; // Placeholder
  }

  // TODO: implement method
  /**
   * Return a number of the app's most recent log messages.
   *
   * @param lineCount the number of log messages to return
   * @return log messages
   */
  public String getApplicationLogs(int lineCount) {
    // Potentially read recent application logs (requires integration with logging framework)
    return "Recent logs..."; // Placeholder
  }

}
