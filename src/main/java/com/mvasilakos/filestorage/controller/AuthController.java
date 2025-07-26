package com.mvasilakos.filestorage.controller;

import com.mvasilakos.filestorage.dto.AuthRequest;
import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.security.JwtUtil;
import com.mvasilakos.filestorage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
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
  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;

  /**
   * Register a new user.
   *
   * @param request new user's information
   * @return new user's information
   */
  @PostMapping("/register")
  public ResponseEntity<JwtResponse> register(@RequestBody AuthRequest request) {
    UserDto registeredUser = userService.registerUser(
        request.username(),
        request.password(),
        request.email());
    final UserDetails userDetails = userService.findUserDetailsByUsername(
        registeredUser.username());
    final String token = jwtUtil.generateToken(userDetails);
    return ResponseEntity.ok(new JwtResponse(token));
  }

  /**
   * Authenticate user and return JWT token.
   *
   * @param request authentication credentials
   * @return JWT token response
   * @throws Exception if authentication fails
   */
  @PostMapping("/login")
  public ResponseEntity<JwtResponse> login(@RequestBody AuthRequest request) throws Exception {
    authenticate(request.username(), request.password());

    final UserDetails userDetails = userService.findUserDetailsByUsername(request.username());

    final String token = jwtUtil.generateToken(userDetails);

    return ResponseEntity.ok(new JwtResponse(token));
  }

  /**
   * Authenticate user credentials.
   *
   * @param username user's username
   * @param password user's password
   * @throws Exception if credentials are invalid
   */
  private void authenticate(String username, String password) throws Exception {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(username, password));
    } catch (BadCredentialsException e) {
      throw new Exception("INVALID_CREDENTIALS", e);
    }
  }

  /**
   * JWT response DTO.
   *
   * @param token JWT token
   */
  public record JwtResponse(String token) {

  }
}