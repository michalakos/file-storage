package com.mvasilakos.filestorage.validator;

import com.mvasilakos.filestorage.exception.InvalidFileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


/**
 * File validation class.
 */
@Component
@RequiredArgsConstructor
public class FileValidator {

  @Value("${file.storage.max-file-size}")
  private int maxFileSize;

  private final Tika tika = new Tika();

  private static final Set<String> ALLOWED_TYPES = Set.of(
      "application/pdf",
      "text/plain",
      "image/jpeg",
      "image/png",
      "application/json"
  );


  /**
   * Make sure the file is of one of the allowed file types.
   *
   * @param file file
   * @throws InvalidFileException thrown if file type is not allowed
   */
  public void validateFile(MultipartFile file) throws InvalidFileException {
    if (file.isEmpty()) {
      throw new InvalidFileException("Cannot store empty file");
    }
    if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
      throw new InvalidFileException("File must have a valid filename");
    }

    try (InputStream is = file.getInputStream()) {
      // Tika detects MIME type based on content, not just filename
      String detectedType = tika.detect(is, file.getOriginalFilename());

      if (!ALLOWED_TYPES.contains(detectedType)) {
        throw new InvalidFileException("File type not allowed: " + detectedType);
      }

      validateFileSize(file);

    } catch (IOException e) {
      throw new InvalidFileException("Could not analyze file");
    }
  }

  private void validateFileSize(MultipartFile file) throws InvalidFileException {
    if (file.isEmpty()) {
      throw new InvalidFileException("File is empty");
    }
    if (file.getSize() == 0) {
      throw new InvalidFileException("File has zero bytes");
    }
    if (file.getSize() > maxFileSize) {
      throw new InvalidFileException(
          String.format("File too large. Maximum allowed size: %s, actual size: %s",
              formatFileSize(maxFileSize),
              formatFileSize(file.getSize())
          )
      );
    }
  }

  private String formatFileSize(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    }
    if (bytes < 1024 * 1024 * 1024) {
      return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
  }

}