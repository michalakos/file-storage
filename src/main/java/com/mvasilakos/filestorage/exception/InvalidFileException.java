package com.mvasilakos.filestorage.exception;

/**
 * Invalid file exception.
 */
public class InvalidFileException extends RuntimeException {

  /**
   * Constructor.
   *
   * @param message message
   */
  public InvalidFileException(String message) {
    super(message);
  }
}
