package com.mvasilakos.filestorage.model;

import java.util.Optional;


/**
 * User roles.
 */
public enum UserRole {
  USER,
  ADMIN;

  /**
   * Attempts to get a UserRole enum value from a string, case-insensitively.
   * Returns an Optional.empty() if no matching role is found.
   *
   * @param roleString The string to search for (e.g., "user", "admin", "USER", "ADMIN").
   * @return An Optional containing the UserRole if a match is found, otherwise Optional.empty().
   */
  public static Optional<UserRole> fromString(String roleString) {
    if (roleString == null || roleString.trim().isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UserRole.valueOf(roleString.trim().toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
