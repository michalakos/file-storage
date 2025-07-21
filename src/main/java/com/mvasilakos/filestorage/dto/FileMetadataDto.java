package com.mvasilakos.filestorage.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;


/**
 * Dto for file metadata.
 *
 * @param id file's id
 * @param filename name of the file
 * @param contentType file type
 * @param size size in bytes
 * @param uploadDate upload date
 * @param storagePath storage path
 * @param userDto details of the user that uploaded it
 */
@Builder(toBuilder = true)
public record FileMetadataDto(
    UUID id,
    String filename,
    String contentType,
    Long size,
    LocalDateTime uploadDate,
    String storagePath,
    UserDto userDto
) {}
