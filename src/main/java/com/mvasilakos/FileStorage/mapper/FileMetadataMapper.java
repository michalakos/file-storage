package com.mvasilakos.FileStorage.mapper;

import com.mvasilakos.FileStorage.dto.FileMetadataDto;
import com.mvasilakos.FileStorage.model.FileMetadata;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@AllArgsConstructor
public class FileMetadataMapper {

    public FileMetadataDto toDto(FileMetadata fileMetadata) {
        return new FileMetadataDto(
            fileMetadata.getId(),
            fileMetadata.getFilename(),
            fileMetadata.getContentType(),
            fileMetadata.getSize(),
            fileMetadata.getUploadDate(),
            fileMetadata.getStoragePath(),
            UserMapper.toDto(fileMetadata.getOwner())
        );
    }

}
