package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.exception.FileEncryptionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Encryption key management service.
 */
@Slf4j
@Getter
@Service
public class SimpleKeyManagementService {

  private static final String KEY_FILE_PATH = "config/encryption.key";
  private final SecretKey secretKey;

  /**
   * Constructor.
   */
  public SimpleKeyManagementService() {
    this.secretKey = loadOrCreateKey();
  }

  private SecretKey loadOrCreateKey() {
    Path keyPath = Paths.get(KEY_FILE_PATH);

    if (Files.exists(keyPath)) {
      try {
        byte[] keyBytes = Files.readAllBytes(keyPath);
        byte[] decodedKey = Base64.getDecoder().decode(keyBytes);
        return new SecretKeySpec(decodedKey, "AES");
      } catch (Exception e) {
        throw new FileEncryptionException("Failed to load encryption key", e);
      }
    } else {
      SecretKey newKey = generateNewKey();
      saveKey(newKey, keyPath);
      return newKey;
    }
  }

  private SecretKey generateNewKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(256);
      return keyGenerator.generateKey();
    } catch (NoSuchAlgorithmException e) {
      throw new FileEncryptionException("Failed to generate AES key", e);
    }
  }

  private void saveKey(SecretKey key, Path keyPath) {
    try {
      Files.createDirectories(keyPath.getParent());

      byte[] encoded = Base64.getEncoder().encode(key.getEncoded());
      Files.write(keyPath, encoded);

      try {
        Files.setPosixFilePermissions(keyPath, Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
        ));
      } catch (UnsupportedOperationException e) {
        log.info("Note: Could not set file permissions (Windows system)");
      }

      log.info("New encryption key saved to: {}", keyPath.toAbsolutePath());

    } catch (IOException e) {
      throw new FileEncryptionException("Failed to save encryption key", e);
    }
  }
}