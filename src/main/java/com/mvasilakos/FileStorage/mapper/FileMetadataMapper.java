package com.mvasilakos.FileStorage.mapper;

import com.mvasilakos.FileStorage.dto.FileMetadataDto;
import com.mvasilakos.FileStorage.model.FileMetadata;
import com.mvasilakos.FileStorage.model.User;
import com.mvasilakos.FileStorage.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class FileMetadataMapper {

    private final UserRepository userRepository;

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

    public FileMetadata fromDto(FileMetadataDto dto) {
        UUID userId = dto.userDto().id();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User with ID " + userId + " not found"));
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(dto.id());
        fileMetadata.setFilename(dto.filename());
        fileMetadata.setContentType(dto.contentType());
        fileMetadata.setSize(dto.size());
        fileMetadata.setUploadDate(dto.uploadDate());
        fileMetadata.setStoragePath(dto.storagePath());
        fileMetadata.setOwner(user);

        return fileMetadata;
    }
}
