package com.mvasilakos.filestorage.exception;

/**
 * Exception for storage limit.
 */
public class StorageLimitExceededException extends RuntimeException {

  /**
   * Constructor.
   *
   * @param message message
   */
  public StorageLimitExceededException(String message) {
    super(message);
  }
}
