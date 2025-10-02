package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the Payment Gateway.
 *
 * Maps application and upstream errors into appropriate HTTP status codes
 * and structured {@link ErrorResponse} payloads so that merchants receive
 * clear, consistent error information:
 * - 400 Bad Request → validation failures (IllegalArgumentException, @Valid errors)
 * - 404 Not Found → when a payment ID cannot be located
 * - 502 Bad Gateway → when the upstream acquiring bank is unavailable
 * - 500 Internal Server Error → for any other unexpected issues
 */

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> notFound(EventProcessingException ex) {
    LOG.error("Not found", ex);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> badRequest(MethodArgumentNotValidException ex) {
    LOG.error("Bad request", ex);
    return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> generic(Exception ex) {
    LOG.error("Unexpected internal error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("Internal server error"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> badInput(IllegalArgumentException ex) {
    LOG.error("Validation error", ex);
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(BankUnavailableException.class)
  public ResponseEntity<ErrorResponse> bankUnavailable(BankUnavailableException ex) {
    LOG.error("Bank unavailable", ex);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("Bank temporarily unavailable"));
  }
}
