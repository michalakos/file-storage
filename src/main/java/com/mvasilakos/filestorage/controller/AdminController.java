package com.mvasilakos.filestorage.controller;

import com.mvasilakos.filestorage.dto.AuthRequest;
import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.service.AdminService;
import com.mvasilakos.filestorage.service.FileService;
import com.mvasilakos.filestorage.service.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Admin controller.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;
  private final FileService fileService;
  private final UserService userService;

  /**
   * Returns all file metadata for all the files uploaded in the server.
   *
   * @return list of file metadata
   */
  @GetMapping("/files")
  public ResponseEntity<List<FileMetadataDto>> getAllFiles() {
    List<FileMetadataDto> files = fileService.listAllFiles();
    return ResponseEntity.ok(files);
  }

  /**
   * Returns all users in the system.
   *
   * @return a list of user details
   */
  @GetMapping("/users")
  public ResponseEntity<List<UserDto>> getAllUsers() {
    List<UserDto> users = userService.getAllUsers();
    return ResponseEntity.ok(users);
  }

  /**
   * Searches for a user with a username or email similar to the given keyword and returns all
   * relevant users.
   *
   * @param keyword search keyword
   * @return a list of user details matching the keyword
   */
  @GetMapping("/users/{keyword}")
  public ResponseEntity<List<UserDto>> searchUser(@PathVariable String keyword) {
    List<UserDto> users = userService.searchUser(keyword);
    return ResponseEntity.ok(users);
  }

  /**
   * Registers a new admin.
   *
   * @param request new admin user's details
   * @return user details
   */
  @PostMapping("/register")
  public ResponseEntity<UserDto> register(@RequestBody AuthRequest request) {
    UserDto registeredUser = adminService.registerAdmin(
        request.username(),
        request.password(),
        request.email());
    return ResponseEntity.ok(registeredUser);
  }

  /**
   * Bans user with the given id.
   *
   * @param id id of the user to ban
   * @return nothing
   */
  @PostMapping("/ban/{id}")
  public ResponseEntity<Void> banUser(@PathVariable UUID id) {
    adminService.banUser(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Unbans user with the given id.
   *
   * @param id id of the user to unban
   * @return nothing
   */
  @PostMapping("/unban/{id}")
  public ResponseEntity<Void> unbanUser(@PathVariable UUID id) {
    adminService.unbanUser(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Changes the role of the user (admin/user).
   *
   * @param id   id of the user whose role will change
   * @param role the role which we want the user to have
   * @return nothing
   */
  @PostMapping("/role/{id}/{role}")
  public ResponseEntity<Void> changeUserRole(@PathVariable UUID id, @PathVariable String role) {
    adminService.changeUserRole(id, role);
    return ResponseEntity.noContent().build();
  }

  /**
   * Find all files exceeding a certain size.
   *
   * @param size the size in bytes
   * @return a list of file metadata
   */
  @GetMapping("/large-files/{size}")
  public ResponseEntity<List<FileMetadataDto>> getLargeFiles(@PathVariable Long size) {
    List<FileMetadataDto> files = adminService.getLargeFilesExceeding(size);
    return ResponseEntity.ok(files);
  }

  /**
   * Calculate the total storage size used for storing files.
   *
   * @return the size in bytes
   */
  @GetMapping("/storage")
  public ResponseEntity<Long> getTotalStorageUsed() {
    Long bytes = adminService.getTotalStorageUsed();
    return ResponseEntity.ok(bytes);
  }

  /**
   * Return the app's most recent logs.
   *
   * @param lines the number of lines of logs to return
   * @return a string containing the logs
   */
  @GetMapping("/logs/{lines}")
  public ResponseEntity<String> getRecentLogs(@PathVariable Integer lines) {
    String logs = adminService.getApplicationLogs(lines);
    return ResponseEntity.ok(logs);
  }

}
