package com.mvasilakos.FileStorage.service;

import com.mvasilakos.FileStorage.dto.FileMetadataDto;
import com.mvasilakos.FileStorage.dto.UserDto;
import com.mvasilakos.FileStorage.model.User;
import com.mvasilakos.FileStorage.model.UserRole;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final FileService fileService;
    private final UserService userService;

    public UserDto registerAdmin(String username, String password, String email) {
        return userService.registerUser(username, password, email, true);
    }

    @Transactional
    public void banUser(UUID userId) {
        User user = userService.findById(userId);
        user.setEnabled(false);
        userService.saveUser(user);
    }

    @Transactional
    public void unbanUser(UUID userId) {
        User user = userService.findById(userId);
        user.setEnabled(true);
        userService.saveUser(user);
    }

    @Transactional
    public void changeUserRole(UUID userId, String newRole) {
        Optional<UserRole> userRoleOptional = UserRole.fromString(newRole);
        UserRole role = userRoleOptional.orElseThrow(
            () -> new IllegalArgumentException("Role " + newRole + " not found"));
        User user = userService.findById(userId);
        user.setRole(role);
        userService.saveUser(user);
    }

    public List<FileMetadataDto> getLargeFilesExceeding(long sizeInBytes) {
        return fileService.findFilesLargerThan(sizeInBytes); // Assuming this method exists in FileService
    }

    public Long getTotalStorageUsed() {
        return fileService.calculateTotalStorageUsage();
    }

    // TODO: implement method
    public Integer getActiveSessionCount() {
        // Query Spring Security or session management for active sessions
        return 0; // Placeholder
    }

    // TODO: implement method
    public String getApplicationLogs(int lineCount) {
        // Potentially read recent application logs (requires integration with logging framework)
        return "Recent logs..."; // Placeholder
    }


}
