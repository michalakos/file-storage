package com.mvasilakos.FileStorage.service;

import com.mvasilakos.FileStorage.model.FileMetadata;
import com.mvasilakos.FileStorage.model.User;
import com.mvasilakos.FileStorage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final FileMetadataRepository fileMetadataRepository;
    private final UserService userService;
    private final Path rootLocation;

    public FileStorageService(
            FileMetadataRepository fileMetadataRepository,
            UserService userService,
            @Value("${storage.location}") String storagePath) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.userService = userService;
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        initStorage();
    }

    private void initStorage() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public FileMetadata storeFile(MultipartFile file, User owner) {
        UUID id = UUID.randomUUID();
        String filename = id + "_" + file.getOriginalFilename();

        try {
            Files.copy(file.getInputStream(), rootLocation.resolve(filename));

            FileMetadata metadata = new FileMetadata();
            metadata.setId(id);
            metadata.setFilename(file.getOriginalFilename());
            metadata.setContentType(file.getContentType());
            metadata.setSize(file.getSize());
            metadata.setUploadDate(LocalDateTime.now());
            metadata.setStoragePath(filename);
            metadata.setOwner(owner);

            return fileMetadataRepository.save(metadata);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Resource loadFileAsResource(UUID fileId, User owner) {
        FileMetadata metadata = fileMetadataRepository.findByIdAndOwner(fileId, owner)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Path filePath = rootLocation.resolve(metadata.getStoragePath());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file", e);
        }
    }

    public List<FileMetadata> listUserFiles(User owner) {
        return fileMetadataRepository.findByOwner(owner);
    }

    public void deleteFile(UUID fileId, User owner) {
        FileMetadata metadata = fileMetadataRepository.findByIdAndOwner(fileId, owner)
                .orElseThrow(() -> new RuntimeException("File not found"));

        try {
            Files.delete(rootLocation.resolve(metadata.getStoragePath()));
            fileMetadataRepository.delete(metadata);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}