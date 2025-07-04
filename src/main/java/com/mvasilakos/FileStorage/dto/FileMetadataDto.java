package com.mvasilakos.FileStorage.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record FileMetadataDto(
    UUID id,
    String filename,
    String contentType,
    Long size,
    LocalDateTime uploadDate,
    String storagePath,
    UserDto userDto
) {}
