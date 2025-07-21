package com.mvasilakos.filestorage.dto;


/**
 * Dto for creating a new user.
 *
 * @param username username
 * @param password password
 * @param email email
 */
public record AuthRequest(String username, String password, String email) {}