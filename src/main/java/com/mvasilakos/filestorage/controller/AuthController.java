package com.mvasilakos.filestorage.controller;

import com.mvasilakos.filestorage.dto.AuthRequest;
import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Authentication controller.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;

  /**
   * Register a new user.
   *
   * @param request new user's information
   * @return new user's information
   */
  @PostMapping("/register")
  public ResponseEntity<UserDto> register(@RequestBody AuthRequest request) {
    UserDto registeredUser = userService.registerUser(
        request.username(),
        request.password(),
        request.email());
    return ResponseEntity.ok(registeredUser);
  }

}


