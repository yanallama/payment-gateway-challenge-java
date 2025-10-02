package com.checkout.payment.gateway.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentValidatorTest {
  @Test
  void validInput_passes() {
    assertDoesNotThrow(() ->
        PaymentValidator.validate("41111111111111", "123", 12, 2029, "GBP", 100)
    );
  }

  @Test
  void invalidPan_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        PaymentValidator.validate("abcd", "123", 12, 2029, "GBP", 100)
    );
  }

  @Test
  void invalidMonth_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        PaymentValidator.validate("41111111111111", "123", 13, 2029, "GBP", 100)
    );
  }

  @Test
  void expiredCard_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        PaymentValidator.validate("41111111111111", "123", 1, 2020, "GBP", 100)
    );
  }

  @Test
  void unsupportedCurrency_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        PaymentValidator.validate("41111111111111", "123", 12, 2029, "XXX", 100)
    );
  }

  @Test
  void invalidCvv_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        PaymentValidator.validate("41111111111111", "12", 12, 2029, "GBP", 100)
    );
  }

  @Test
  void nonPositiveAmount_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        PaymentValidator.validate("41111111111111", "123", 12, 2029, "GBP", 0)
    );
  }
}
