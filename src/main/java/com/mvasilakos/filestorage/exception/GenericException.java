package com.mvasilakos.filestorage.exception;

/**
 * Generic exception.
 */
public class GenericException extends RuntimeException {

  /**
   * Constructor.
   *
   * @param message message
   */
  public GenericException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message message
   */
  public GenericException(String message, Throwable e) {
    super(message, e);
  }

}
