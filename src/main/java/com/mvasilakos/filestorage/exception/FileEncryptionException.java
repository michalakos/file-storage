package com.mvasilakos.filestorage.exception;

/**
 * File encryption exception.
 */
public class FileEncryptionException extends RuntimeException {

  /**
   * Constructor.
   *
   * @param message message
   */
  public FileEncryptionException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message message
   */
  public FileEncryptionException(String message, Throwable cause) {
    super(message, cause);
  }

}
