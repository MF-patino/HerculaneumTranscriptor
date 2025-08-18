package com.mf.HerculaneumTranscriptor.exception;
/**
 * This exception is thrown when a user tries to register with an already used username or email.
 */
public class UserAlreadyExistsException extends RuntimeException {

  public UserAlreadyExistsException(String message) {
    super(message);
  }
}