package com.mvasilakos.filestorage.mapper;

import com.mvasilakos.filestorage.dto.FileMetadataDto;
import com.mvasilakos.filestorage.model.FileMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * Mapper for converting file metadata.
 */
@Component
@RequiredArgsConstructor
public class FileMetadataMapper implements EntityMapper<FileMetadata, FileMetadataDto> {

  private final UserMapper userMapper;

  @Override
  public FileMetadataDto toDto(FileMetadata entity) {
    if (entity == null) {
      return null;
    }

    return FileMetadataDto.builder()
        .id(entity.getId())
        .filename(entity.getFilename())
        .contentType(entity.getContentType())
        .size(entity.getSize())
        .originalFileSize(entity.getOriginalFileSize())
        .uploadDate(entity.getUploadDate())
        .storagePath(entity.getStoragePath())
        .userDto(userMapper.toDto(entity.getOwner()))
        .build();
  }

  @Override
  public FileMetadata toEntity(FileMetadataDto dto) {
    if (dto == null) {
      return null;
    }

    FileMetadata entity = new FileMetadata();
    entity.setId(dto.id());
    entity.setFilename(dto.filename());
    entity.setContentType(dto.contentType());
    entity.setSize(dto.size());
    entity.setUploadDate(dto.uploadDate());
    return entity;
  }

}
