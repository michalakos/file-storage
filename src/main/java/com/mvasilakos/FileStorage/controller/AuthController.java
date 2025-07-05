package com.mvasilakos.FileStorage.controller;

import com.mvasilakos.FileStorage.dto.AuthRequestDto;
import com.mvasilakos.FileStorage.dto.UserDto;
import com.mvasilakos.FileStorage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody AuthRequestDto request) {
        UserDto registeredUser = userService.registerUser(request.username(), request.password(), request.email());
        return ResponseEntity.ok(registeredUser);
    }

}


