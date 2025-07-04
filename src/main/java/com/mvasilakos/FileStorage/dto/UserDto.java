package com.mvasilakos.FileStorage.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserDto(
    UUID id,
    String username
) {}
