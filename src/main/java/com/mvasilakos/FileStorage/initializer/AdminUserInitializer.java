package com.mvasilakos.FileStorage.initializer;

import com.mvasilakos.FileStorage.model.User;
import com.mvasilakos.FileStorage.model.UserRole;
import com.mvasilakos.FileStorage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        createAdminUser();
    }

    private void createAdminUser() {
        if (userRepository.existsByUsername(adminUsername)) return;

        String password = adminPassword.isEmpty() ? generateRandomPassword() : adminPassword;
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);

        System.out.println("=================================");
        System.out.println("Admin user created successfully!");
        System.out.println("Username: " + adminUsername);
        System.out.println("Email: " + adminEmail);
        System.out.println("Password: " + password);
        System.out.println("=================================");
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

}