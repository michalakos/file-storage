package com.mvasilakos.FileStorage.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileMetadataDto(
    UUID id,
    String filename,
    String contentType,
    Long size,
    LocalDateTime uploadDate,
    String storagePath,
    UserDto userDto
) {}
