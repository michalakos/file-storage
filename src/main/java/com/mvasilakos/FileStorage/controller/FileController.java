package com.mvasilakos.FileStorage.controller;

import com.mvasilakos.FileStorage.model.FileMetadata;
import com.mvasilakos.FileStorage.model.User;
import com.mvasilakos.FileStorage.service.FileStorageService;
import com.mvasilakos.FileStorage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storageService;
    private final UserService userService;

    @PostMapping
    public FileMetadata uploadFile(@RequestParam("file") MultipartFile file, Principal principal) {
        User owner = userService.getAuthenticatedUser(principal);
        return storageService.storeFile(file, owner);
    }

    @GetMapping
    public List<FileMetadata> listFiles(Principal principal) {
        User owner = userService.getAuthenticatedUser(principal);
        return storageService.listUserFiles(owner);
    }

    @GetMapping("/{id}/data")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id, Principal principal) {
        User owner = userService.getAuthenticatedUser(principal);
        Resource resource = storageService.loadFileAsResource(id, owner);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable UUID id, Principal principal) {
        User owner = userService.getAuthenticatedUser(principal);
        storageService.deleteFile(id, owner);
        return ResponseEntity.noContent().build();
    }
}