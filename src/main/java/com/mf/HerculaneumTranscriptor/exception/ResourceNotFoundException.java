package com.mf.HerculaneumTranscriptor.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This exception is thrown when a requested resource is not found in the system.
 * The @ResponseStatus annotation tells Spring to automatically translate this exception
 * into an HTTP 404 response, simplifying controller-level error handling.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}