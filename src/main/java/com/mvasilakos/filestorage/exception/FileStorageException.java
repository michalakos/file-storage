package com.mvasilakos.filestorage.exception;

/**
 * File storage exception.
 */
public class FileStorageException extends RuntimeException {

  /**
   * Constructor.
   *
   * @param message message
   */
  public FileStorageException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message message
   * @param cause cause
   */
  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
