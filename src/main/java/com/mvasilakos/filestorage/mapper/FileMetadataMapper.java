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
        .uploadDate(entity.getUploadDate())
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
