package com.mvasilakos.filestorage.security;

import com.mvasilakos.filestorage.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * JWT utility service.
 */
@Service
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private Long expiration;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes());
  }

  /**
   * Extract username from token.
   *
   * @param token jwt token
   * @return username
   */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Extract role from token.
   *
   * @param token jwt token
   * @return user role
   */
  public String extractRole(String token) {
    return extractClaim(token, claims -> (String) claims.get("role"));
  }

  /**
   * Extract user ID from token.
   *
   * @param token jwt token
   * @return user ID
   */
  public String extractUserId(String token) {
    return extractClaim(token, claims -> (String) claims.get("userId"));
  }

  /**
   * Extract email from token.
   *
   * @param token jwt token
   * @return user email
   */
  public String extractEmail(String token) {
    return extractClaim(token, claims -> (String) claims.get("email"));
  }

  /**
   * Extract expiration date from token.
   *
   * @param token jwt token
   * @return expiration date
   */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Extract claim from token.
   *
   * @param token          jwt token
   * @param claimsResolver claims resolver
   * @param <T>            type parameter
   * @return claim
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * Generate new token with user details and role.
   *
   * @param userDetails user details
   * @return jwt token
   */
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();

    if (userDetails instanceof User user) {
      claims.put("role", user.getRole().name().toLowerCase());
      claims.put("userId", user.getId().toString());
      claims.put("email", user.getEmail());
      claims.put("enabled", user.isEnabled());
    }

    return createToken(claims, userDetails.getUsername());
  }

  /**
   * Generate token with custom claims.
   *
   * @param extraClaims additional claims to include
   * @param userDetails user details
   * @return jwt token
   */
  public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>(extraClaims);

    // Add user information to claims if userDetails is our User entity
    if (userDetails instanceof User user) {
      claims.put("role", user.getRole().name().toLowerCase());
      claims.put("userId", user.getId().toString());
      claims.put("email", user.getEmail());
      claims.put("enabled", user.isEnabled());
    }

    return createToken(claims, userDetails.getUsername());
  }

  private String createToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Validate token.
   *
   * @param token       jwt token
   * @param userDetails user details
   * @return true if user is valid
   */
  public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  /**
   * Check if user has admin role based on token.
   *
   * @param token jwt token
   * @return true if user is admin
   */
  public Boolean isAdmin(String token) {
    String role = extractRole(token);
    return "admin".equals(role);
  }

  /**
   * Check if user has user role based on token.
   *
   * @param token jwt token
   * @return true if user is regular user
   */
  public Boolean isUser(String token) {
    String role = extractRole(token);
    return "user".equals(role);
  }

}