package com.checkout.payment.gateway.validation;

import java.time.YearMonth;
import java.util.Set;

public final class PaymentValidator {

  private static final Set<String> ALLOWED = Set.of("GBP", "EUR", "USD");

  public static void validate(String pan, String cvv, int month, int year, String currency, int amount) {
    if (pan == null || !pan.matches("\\d{14,19}")) { // primary account number
      throw bad("Invalid card number");
    }
    if (cvv == null || !cvv.matches("\\d{3,4}")) {
      throw bad("Invalid CVV");
    }
    if (month < 1 || month > 12) {
      throw bad("Invalid expiry month");
    }
    var now = YearMonth.now();
    var exp = YearMonth.of(year, month);
    if (exp.isBefore(now)) {
      throw bad("Card expired");
    }
    if (currency == null || currency.length() != 3 || !ALLOWED.contains(currency)) {
      throw bad("Unsupported currency");
    }
    if (amount <= 0) {
      throw bad("Amount must be positive");
    }
  }

  public static int last4(String pan) {
    return Integer.parseInt(pan.substring(pan.length() - 4));
  }

  private static IllegalArgumentException bad(String m) {
    return new IllegalArgumentException(m);
  }
}
