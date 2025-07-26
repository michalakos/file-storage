package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.exception.GenericException;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.model.UserRole;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


/**
 * Admin service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

  @Value("${logging.file.name:}")
  private String logfile;

  private final FileService fileService;
  private final UserService userService;


  /**
   * Register a new admin.
   *
   * @param username username
   * @param password password
   * @param email    email address
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
   * @param userId  id of the user
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

  /**
   * Return a number of the app's most recent log messages.
   *
   * @param lineCount the number of log messages to return
   * @return log messages
   */
  public String getApplicationLogs(int lineCount) {
    if (logfile.isEmpty()) {
      throw new GenericException("No log file found");
    }

    Path logFilePath = Paths.get(logfile);
    try (BufferedReader reader = Files.newBufferedReader(logFilePath)) {
      return reader.lines().limit(lineCount)
          .collect(Collectors.joining(System.lineSeparator()));

    } catch (IOException e) {
      throw new GenericException("Couldn't read log file", e);
    }
  }

}
