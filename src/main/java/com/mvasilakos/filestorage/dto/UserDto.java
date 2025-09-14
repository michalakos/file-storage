package com.mvasilakos.filestorage.dto;

import java.util.UUID;
import lombok.Builder;

/**
 * Dto for user details.
 *
 * @param id       user id
 * @param username username
 * @param email    email
 */
@Builder
public record UserDto(
    UUID id,
    String username,
    String email,
    boolean isAdmin,
    boolean isBanned
) {

}
