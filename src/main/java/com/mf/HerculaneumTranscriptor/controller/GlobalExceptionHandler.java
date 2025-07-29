package com.mf.HerculaneumTranscriptor.controller;

import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.exception.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
  private ResponseEntity<Object> buildResponseBody(HttpStatus status, String errorType, String message) {
    // Create a clear, structured JSON error response body
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", status.value());
    body.put("error", errorType);
    body.put("message", message);

    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<Object> handleUserAlreadyExists(
          UserAlreadyExistsException ex, WebRequest request) {

    return buildResponseBody(HttpStatus.CONFLICT, "User conflict", ex.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {

    return buildResponseBody(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex, WebRequest request) {

    return buildResponseBody(HttpStatus.FORBIDDEN, "Bad credentials", ex.getMessage());
  }
}