package com.mvasilakos.FileStorage.dto;

import java.util.UUID;

public record UserDto(
    UUID id,
    String username
) {}
