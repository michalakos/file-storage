package com.mvasilakos.FileStorage.controller;

import com.mvasilakos.FileStorage.dto.AuthRequestDto;
import com.mvasilakos.FileStorage.dto.FileMetadataDto;
import com.mvasilakos.FileStorage.dto.UserDto;
import com.mvasilakos.FileStorage.service.AdminService;
import com.mvasilakos.FileStorage.service.FileService;
import com.mvasilakos.FileStorage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final FileService fileService;
    private final UserService userService;

    @GetMapping("/files")
    public ResponseEntity<List<FileMetadataDto>> getAllFiles() {
        List<FileMetadataDto> files = fileService.listAllFiles();
        return ResponseEntity.ok(files);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{keyword}")
    public ResponseEntity<List<UserDto>> searchUser(@PathVariable String keyword) {
        List<UserDto> users = userService.searchUser(keyword);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody AuthRequestDto request) {
        UserDto registeredUser = adminService.registerAdmin(request.username(), request.password(), request.email());
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/ban/{id}")
    public ResponseEntity<Void> banUser(@PathVariable UUID id) {
        adminService.banUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unban/{id}")
    public ResponseEntity<Void> unbanUser(@PathVariable UUID id) {
        adminService.unbanUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/role/{id}/{role}")
    public ResponseEntity<Void> changeUserRole(@PathVariable UUID id, @PathVariable String role) {
        adminService.changeUserRole(id, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/large-files/{size}")
    public ResponseEntity<List<FileMetadataDto>> getLargeFiles(@PathVariable Long size) {
        List<FileMetadataDto> files = adminService.getLargeFilesExceeding(size);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/storage")
    public ResponseEntity<Long> getTotalStorageUsed() {
        Long bytes = adminService.getTotalStorageUsed();
        return ResponseEntity.ok(bytes);
    }

    @GetMapping("/sessions")
    public ResponseEntity<Integer> getActiveSessionCount() {
        Integer sessions = adminService.getActiveSessionCount();
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/logs/{lines}")
    public ResponseEntity<String> getRecentLogs(@PathVariable Integer lines) {
        String logs = adminService.getApplicationLogs(lines);
        return ResponseEntity.ok(logs);
    }

}
