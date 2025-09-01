package com.mf.HerculaneumTranscriptor.controller;

import com.mf.HerculaneumTranscriptor.exception.ResourceNotFoundException;
import com.mf.HerculaneumTranscriptor.exception.ResourceAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ValidationException;
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

  @ExceptionHandler(ResourceAlreadyExistsException.class)
  public ResponseEntity<Object> handleUserAlreadyExists(
          ResourceAlreadyExistsException ex, WebRequest request) {

    return buildResponseBody(HttpStatus.CONFLICT, "Resource conflict", ex.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {

    return buildResponseBody(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex, WebRequest request) {

    return buildResponseBody(HttpStatus.UNAUTHORIZED, "Bad credentials", ex.getMessage());
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<Object> handleAuthorizationDenied(AuthorizationDeniedException ex, WebRequest request) {

    return buildResponseBody(HttpStatus.FORBIDDEN, "Authorization denied", "Access has been denied");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidationError(MethodArgumentNotValidException ex, WebRequest request) {

    return buildResponseBody(HttpStatus.BAD_REQUEST, "Validation error", ex.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<Object> handleValidationException(ValidationException ex, WebRequest request) {

    return buildResponseBody(HttpStatus.BAD_REQUEST, "Validation error", ex.getMessage());
  }
}