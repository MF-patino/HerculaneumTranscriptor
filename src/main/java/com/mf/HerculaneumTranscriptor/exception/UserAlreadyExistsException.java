package com.mf.HerculaneumTranscriptor.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This exception is thrown when a user tries to register with an already used username or email.
 * The @ResponseStatus annotation tells Spring to automatically translate this exception
 * into an HTTP 33 response, simplifying controller-level error handling.
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {

  public UserAlreadyExistsException(String message) {
    super(message);
  }
}