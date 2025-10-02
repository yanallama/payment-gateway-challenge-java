package com.checkout.payment.gateway.exception;

public class BankUnavailableException extends RuntimeException {
  public BankUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
  public BankUnavailableException(String message) {
    super(message);
  }
}
