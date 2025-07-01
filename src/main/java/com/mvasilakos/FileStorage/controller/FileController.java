package com.mvasilakos.FileStorage.controller;

import com.mvasilakos.FileStorage.dto.FileMetadataDto;
import com.mvasilakos.FileStorage.model.User;
import com.mvasilakos.FileStorage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storageService;

    @PostMapping
    public FileMetadataDto uploadFile(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal User owner) {
        return storageService.storeFile(file, owner);
    }

    @GetMapping
    public List<FileMetadataDto> listFiles(@AuthenticationPrincipal User owner) {
        return storageService.listUserFiles(owner);
    }

    @GetMapping("/{id}/data")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id, @AuthenticationPrincipal User owner) {
        Resource resource = storageService.loadFileAsResource(id, owner);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable UUID id, @AuthenticationPrincipal User owner) {
        storageService.deleteFile(id, owner);
        return ResponseEntity.noContent().build();
    }
}