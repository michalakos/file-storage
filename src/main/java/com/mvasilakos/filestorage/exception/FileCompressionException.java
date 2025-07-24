package com.mvasilakos.filestorage.exception;

/**
 * File compression exception.
 */
public class FileCompressionException extends RuntimeException {

  /**
   * Constructor.
   *
   * @param message message
   */
  public FileCompressionException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message message
   */
  public FileCompressionException(String message, Throwable cause) {
    super(message, cause);
  }
}
