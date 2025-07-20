package com.mvasilakos.filestorage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Global exception handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Returns an HTTP bad request response in case of a runtime exception.
   *
   * @param ex raised exception
   * @return HTTP response entity
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }

  /**
   * Returns an HTTP forbidden response in case of access denied exception.
   *
   * @return HTTP response entity
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<String> handleAccessDenied() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
  }
}