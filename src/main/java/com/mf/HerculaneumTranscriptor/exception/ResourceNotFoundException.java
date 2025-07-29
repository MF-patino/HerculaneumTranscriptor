package com.mf.HerculaneumTranscriptor.exception;

/**
 * This exception is thrown when a requested resource is not found in the system.
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}