package com.mvasilakos.FileStorage.mapper;

import com.mvasilakos.FileStorage.dto.FileMetadataDto;
import com.mvasilakos.FileStorage.model.FileMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class FileMetadataMapper implements EntityMapper<FileMetadata, FileMetadataDto> {

    public FileMetadataDto toDto(FileMetadata entity) {
        if (entity == null) return null;

        return FileMetadataDto.builder()
            .id(entity.getId())
            .filename(entity.getFilename())
            .contentType(entity.getContentType())
            .size(entity.getSize())
            .uploadDate(entity.getUploadDate())
            .build();
    }

    public FileMetadata toEntity(FileMetadataDto dto) {
        if (dto == null) return null;
        FileMetadata entity = new FileMetadata();
        entity.setId(dto.id());
        entity.setFilename(dto.filename());
        entity.setContentType(dto.contentType());
        entity.setSize(dto.size());
        entity.setUploadDate(dto.uploadDate());
        return entity;
    }

}
