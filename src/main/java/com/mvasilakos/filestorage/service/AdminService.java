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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
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

  @Value("${app.admin.username:admin}")
  private String adminUsername;

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
   * Delete user.
   *
   * @param userId userId of the user to delete
   */
  public void deleteUser(UUID userId) {
    User user = userService.findById(userId);
    if (adminUsername.equals(user.getUsername())) {
      log.error("Cannot delete user {}", user.getUsername());
      throw new GenericException(String.format("Cannot delete user %s", user.getUsername()));
    }
    userService.deleteUsersAccount(userId);
  }

  /**
   * Ban user.
   *
   * @param userId id of the user to ban
   */
  @Transactional
  public void banUser(UUID userId) {
    User user = userService.findById(userId);
    if (!user.isEnabled()) {
      log.error("User with ID {} is already banned", userId);
      throw new GenericException(String.format("User with ID %s is already banned", userId));
    }
    if (adminUsername.equals(user.getUsername())) {
      log.error("Cannot ban user {}", user.getUsername());
      throw new GenericException(String.format("Cannot ban user %s", user.getUsername()));
    }
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
    if (user.isEnabled()) {
      log.error("User with ID {} is not banned", userId);
      throw new GenericException(String.format("User with ID %s is not banned", userId));
    }
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

    if (adminUsername.equals(user.getUsername())) {
      log.error("Cannot change role of user {}", user.getUsername());
      throw new GenericException(String.format("Cannot ban user %s", user.getUsername()));
    }
    if (user.getRole().equals(role)) {
      log.error("User with ID {} is already a {}", userId, role);
      throw new GenericException(String.format("User with ID %s is already a %s", userId, role));
    }
    user.setRole(role);
    userService.saveUser(user);
  }

  /**
   * Toggle user role (e.g. from user to admin).
   *
   * @param userId id of the user
   */
  @Transactional
  public void toggleUserRole(UUID userId) {
    User user = userService.findById(userId);
    if (adminUsername.equals(user.getUsername())) {
      log.error("Cannot toggle role of user {}", user.getUsername());
      throw new GenericException(String.format("Cannot ban user %s", user.getUsername()));
    }

    UserRole newRole = (user.getRole().equals(UserRole.USER)) ? UserRole.ADMIN : UserRole.USER;
    log.info("Making user {} with ID {} a {}", user.getUsername(), userId, newRole);
    user.setRole(newRole);
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
      List<String> allLines = reader.lines().collect(Collectors.toList());
      List<String> logEntries = parseLogEntries(allLines);

      int startIndex = Math.max(0, logEntries.size() - lineCount);
      List<String> recentEntries = logEntries.subList(startIndex, logEntries.size());
      Collections.reverse(recentEntries);
      return recentEntries.stream()
          .collect(Collectors.joining(System.lineSeparator()));

    } catch (IOException e) {
      throw new GenericException("Couldn't read log file", e);
    }
  }

  private List<String> parseLogEntries(List<String> lines) {
    List<String> logEntries = new ArrayList<>();
    StringBuilder currentEntry = new StringBuilder();
    // Pattern to match the start of a new log entry based on log pattern
    Pattern logStartPattern = Pattern.compile(
        "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\[.+?] \\w+\\s+.+? - .+$");

    for (String line : lines) {
      if (logStartPattern.matcher(line).matches()) {
        // This is the start of a new log entry
        if (!currentEntry.isEmpty()) {
          // Save the previous complete entry
          logEntries.add(currentEntry.toString().trim());
          currentEntry.setLength(0);
        }
        currentEntry.append(line);
      } else if (!currentEntry.isEmpty()) {
        // This is a continuation line
        currentEntry.append(System.lineSeparator()).append(line);
      }
      // Ignore lines that don't match and aren't continuations
    }
    // Add last entry
    if (!currentEntry.isEmpty()) {
      logEntries.add(currentEntry.toString().trim());
    }
    return logEntries;
  }
}
