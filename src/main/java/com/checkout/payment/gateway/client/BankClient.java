package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class BankClient {

  private static final Logger LOG = LoggerFactory.getLogger(BankClient.class);

  private final String bankUrl;
  private final RestTemplate rest;

  public BankClient(RestTemplate rest, @Value("${bank.url:http://localhost:8080/payments}") String bankUrl) {
    this.rest = rest;
    this.bankUrl = bankUrl;
  }

  public BankResultType authorize(String pan, int expiryMonth, int expiryYear, String currency,
      int amount, String cvv) {
    // the exact body the simulator expects
    String expiryDate = String.format("%02d/%d", expiryMonth, expiryYear);
    Map<String, Object> body = Map.of(
        "card_number", pan,
        "expiry_date", expiryDate,
        "currency", currency,
        "amount", amount,
        "cvv", cvv
    );

    try {
      LOG.debug("Calling acquiring bank at {}", bankUrl);
      var resp = rest.postForEntity(bankUrl, body, BankAuthResponse.class).getBody();

      if (resp == null || resp.authorized() == null) {
        LOG.error("Bank response malformed or empty");
        throw new BankUnavailableException("Bank response malformed or empty");
      }

      var outcome = resp.authorized() ? BankResultType.AUTHORIZED : BankResultType.DECLINED;
      LOG.debug("Bank responded with outcome {}", outcome);
      return outcome;

    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() == 503) {
        throw new BankUnavailableException("Bank service unavailable (503).", e);
      }
      throw e; // generic

    } catch (RestClientException e) {
      // Network/timeout/etc
      throw new BankUnavailableException("Bank connectivity error: " + e.getMessage(), e);
    }
  }

  record BankAuthResponse(Boolean authorized, String authorization_code) {}

  public enum BankResultType { AUTHORIZED, DECLINED }
}