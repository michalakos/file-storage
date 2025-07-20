package com.mvasilakos.filestorage.dto;


/**
 * Dto for creating a new user.
 *
 * @param username username
 * @param password password
 * @param email email
 */
public record AuthRequestDto(String username, String password, String email) {}