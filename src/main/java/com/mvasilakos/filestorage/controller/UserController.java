package com.mvasilakos.filestorage.controller;

import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.model.User;
import com.mvasilakos.filestorage.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /**
   * Get account details.
   *
   * @param user authenticated user
   * @return account details
   */
  @GetMapping("/account")
  public ResponseEntity<UserDto> getAccountDetails(@AuthenticationPrincipal User user) {
    UserDto accountDetails = userService.getAccountDetails(user.getId());
    return ResponseEntity.ok(accountDetails);
  }

  /**
   * Delete own account.
   *
   * @param user authenticated user
   * @return nothing
   */
  @DeleteMapping("/account")
  public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
    userService.deleteOwnAccount(user);
    return ResponseEntity.noContent().build();
  }

  /**
   * Searches for a user with a username or email similar to the given keyword and returns all
   * relevant users.
   *
   * @param keyword search keyword
   * @return a list of user details matching the keyword
   */
  @GetMapping
  public ResponseEntity<List<UserDto>> searchUser(@AuthenticationPrincipal User user,
      @RequestParam String keyword) {
    List<UserDto> users = userService.searchUser(keyword).stream()
        .filter(userDto -> !user.getId().equals(userDto.id())).toList();
    return ResponseEntity.ok(users);
  }

  /**
   * Get total used storage for user.
   *
   * @param user authenticated user
   * @return bytes of used storage
   */
  @GetMapping("/storage/used")
  public ResponseEntity<Long> getStorageUsed(@AuthenticationPrincipal User user) {
    long storageUsed = userService.getUserStorageUsed(user);
    return ResponseEntity.ok(storageUsed);
  }

  /**
   * Get maximum allowed storage per user.
   *
   * @return bytes of maximum storage
   */
  @GetMapping("/storage/user-max")
  public ResponseEntity<Long> getMaxStoragePerUser(@AuthenticationPrincipal User user) {
    long maxStoragePerUser = userService.getStorageLimitPerUser(user);
    return ResponseEntity.ok(maxStoragePerUser);
  }
}
