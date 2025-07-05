package com.mvasilakos.FileStorage.service;

import com.mvasilakos.FileStorage.dto.UserDto;
import com.mvasilakos.FileStorage.mapper.UserMapper;
import com.mvasilakos.FileStorage.model.User;
import com.mvasilakos.FileStorage.model.UserRole;
import com.mvasilakos.FileStorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    public UserDto registerUser(String username, String password, String email) {
        validateNewUser(username, password, email);
        User user = createUser(username, password, email);

        User registeredUser = userRepository.save(user);
        return userMapper.toDto(registeredUser);
    }

    public UserDto registerUser(String username, String password, String email, boolean isAdmin) {
        validateNewUser(username, password, email);
        User user = createUser(username, password, email);
        if (isAdmin) {
            user.setRole(UserRole.ADMIN);
        }
        User registeredUser = userRepository.save(user);
        return userMapper.toDto(registeredUser);
    }

    // FIXME: add password validation
    private void validateNewUser(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        } else if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        } else if (!EmailValidator.getInstance().isValid(email)) {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    private User createUser(String username, String password, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        return user;
    }

    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toDtoList(users);
    }

    public List<UserDto> searchUser(String searchTerm) {
        List<User> users = userRepository.searchUser(searchTerm);
        return userMapper.toDtoList(users);
    }

    protected User findById(UUID userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    protected User findByUsername(String userId) {
        Optional<User> userOptional = userRepository.findByUsername(userId);
        return userOptional.orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

}