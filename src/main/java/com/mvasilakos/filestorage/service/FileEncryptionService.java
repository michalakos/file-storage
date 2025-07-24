package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.exception.FileEncryptionException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.springframework.stereotype.Service;

/**
 * File encryption service.
 */
@Service
public class FileEncryptionService {

  private final SecretKey secretKey;

  /**
   * Constructor.
   *
   * @param keyManagementService keyManagementService
   */
  public FileEncryptionService(SimpleKeyManagementService keyManagementService) {
    this.secretKey = keyManagementService.getSecretKey();
  }

  /**
   * Decrypt byte array.
   *
   * @param encryptedData byte array
   * @param iv            initialization vector
   * @return byte array
   */
  public byte[] decrypt(byte[] encryptedData, byte[] iv) {
    try {
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

      return cipher.doFinal(encryptedData);
    } catch (Exception e) {
      throw new FileEncryptionException("Failed to decrypt data", e);
    }
  }

  /**
   * Encrypt data stream.
   *
   * @param inputStream unencrypted data stream
   * @return encrypted data stream
   */
  public EncryptionResult encryptStream(InputStream inputStream) {
    try {
      byte[] iv = generateIv();
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

      ByteArrayOutputStream encryptedBytesBuffer = new ByteArrayOutputStream();
      try (CipherOutputStream cipherOutputStream = new CipherOutputStream(encryptedBytesBuffer,
          cipher)) {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          cipherOutputStream.write(buffer, 0, bytesRead);
        }
        cipherOutputStream.flush();
      }

      return new EncryptionResult(encryptedBytesBuffer.toByteArray(), iv);
    } catch (Exception e) {
      throw new FileEncryptionException("Failed to encrypt stream", e);
    }
  }

  private byte[] generateIv() {
    byte[] iv = new byte[16];
    new SecureRandom().nextBytes(iv);
    return iv;
  }

  /**
   * Encryption result class.
   */
  public record EncryptionResult(byte[] encryptedData, byte[] iv) {

  }
}