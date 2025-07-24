package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.exception.FileStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * File storage service.
 */
@Service
public class FileStorageService {

  private final Path rootLocation;

  /**
   * Constructor.
   *
   * @param storageLocation file storage location
   */
  public FileStorageService(@Value("${app.storage.location}") String storageLocation) {
    this.rootLocation = Paths.get(storageLocation);
  }

  /**
   * Store encrypted file.
   *
   * @param storagePath             storage path
   * @param iv                      initialization vector
   * @param encryptedCompressedData encrypted data
   */
  public void storeEncryptedFile(String storagePath, byte[] iv, byte[] encryptedCompressedData) {
    try {
      Path fullStoragePath = rootLocation.resolve(storagePath);
      Files.createDirectories(fullStoragePath.getParent());

      try (OutputStream fileOutputStream = Files.newOutputStream(fullStoragePath)) {
        fileOutputStream.write(iv);
        fileOutputStream.write(encryptedCompressedData);
      }
    } catch (IOException e) {
      throw new FileStorageException("Failed to store encrypted file", e);
    }
  }

  /**
   * Retrieve encrypted file.
   *
   * @param storagePath storage path
   * @return encrypted file data
   */
  public StoredFileData readEncryptedFile(String storagePath) {
    try {
      Path fullStoragePath = rootLocation.resolve(storagePath);

      if (!Files.exists(fullStoragePath)) {
        throw new FileStorageException("Stored file not found on disk at path: " + fullStoragePath);
      }
      if (!Files.isReadable(fullStoragePath)) {
        throw new FileStorageException("File is not readable at path: " + fullStoragePath);
      }

      try (InputStream fileInputStream = Files.newInputStream(fullStoragePath)) {
        byte[] iv = new byte[16];
        int bytesRead = fileInputStream.read(iv);
        if (bytesRead != 16) {
          throw new IOException("Could not read full IV from file. "
              + "File might be corrupted or not properly encrypted.");
        }

        byte[] compressedEncryptedData = fileInputStream.readAllBytes();

        return new StoredFileData(iv, compressedEncryptedData);
      }
    } catch (IOException e) {
      throw new FileStorageException("Failed to read encrypted file", e);
    }
  }

  /**
   * Get stored file size.
   *
   * @param storagePath storage path
   * @return size in bytes
   */
  public long getFileSize(String storagePath) {
    try {
      Path fullStoragePath = rootLocation.resolve(storagePath);
      return Files.size(fullStoragePath);
    } catch (IOException e) {
      throw new FileStorageException("Failed to get file size", e);
    }
  }

  /**
   * Delete file data.
   *
   * @param storagePath storage path
   */
  public void deleteFile(String storagePath) {
    try {
      Path fullStoragePath = rootLocation.resolve(storagePath);
      Files.deleteIfExists(fullStoragePath);
    } catch (IOException e) {
      throw new FileStorageException("Failed to delete file", e);
    }
  }

  /**
   * Stored file data class.
   */
  public record StoredFileData(byte[] iv, byte[] compressedEncryptedData) {

  }
}