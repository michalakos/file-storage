package com.mvasilakos.FileStorage.controller;

import com.mvasilakos.FileStorage.dto.FileMetadataDto;
import com.mvasilakos.FileStorage.model.User;
import com.mvasilakos.FileStorage.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    private final FileService storageService;

    @PostMapping
    public ResponseEntity<FileMetadataDto> uploadFile(@RequestParam("file") MultipartFile file,
                                                      @AuthenticationPrincipal User owner) {
        FileMetadataDto metadata = storageService.storeFile(file, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataDto>> listFiles(@AuthenticationPrincipal User user) {
        List<FileMetadataDto> metadata =  storageService.listUserFiles(user);
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileMetadataDto> getFileMetadata(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        FileMetadataDto metadata = storageService.getFileMetadata(id, user);
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/{id}/data")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        Resource resource = storageService.loadFileAsResource(id, user);
        FileMetadataDto metadata = storageService.getFileMetadata(id, user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, metadata.contentType())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id, @AuthenticationPrincipal User owner) {
        storageService.deleteFile(id, owner);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/share/{fileId}/{username}/{readOnly}")
    public ResponseEntity<Boolean> shareFile(@PathVariable UUID fileId,
                                             @PathVariable String username,
                                             @PathVariable boolean readOnly,
                                             @AuthenticationPrincipal User owner) {
        Boolean result = storageService.shareFile(fileId, username, readOnly, owner);
        return ResponseEntity.ok(result);
    }
}