package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import com.checkout.payment.gateway.validation.PaymentValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);
  private final PaymentsRepository repo;
  private final BankClient bank;

  public PaymentGatewayService(PaymentsRepository repo, BankClient bank) {
    this.repo = repo;
    this.bank = bank;
  }

  public PaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return repo.get(id).orElseThrow(() -> new EventProcessingException("Payment not found"));
  }

  public PaymentResponse processPayment(PaymentRequest req) {
    // validate gateway-side
      PaymentValidator.validate(req.getCardNumber(), req.getCvv(), req.getExpiryMonth(),
          req.getExpiryYear(), req.getCurrency(), req.getAmount());

    // call bank
    var result = bank.authorize(req.getCardNumber(), req.getExpiryMonth(), req.getExpiryYear(),
        req.getCurrency(), req.getAmount(), req.getCvv());
    // safe logging: never log PAN/CVV/amount values
    LOG.debug("Bank outcome: {} (card ****{})", result, PaymentValidator.last4(req.getCardNumber()));

    // map & persist
    var id = UUID.randomUUID();
    var status = (result == BankClient.BankResultType.AUTHORIZED)
        ? PaymentStatus.AUTHORIZED
        : PaymentStatus.DECLINED;

    LOG.debug("Persisting payment {} with status {}", id, status);
    var saved = buildResponse(id, status, req);
    repo.add(saved);
    return saved;
  }

  // note: only last-4 is persisted & also we don't store CVV/PAN
  private PaymentResponse buildResponse(UUID id, PaymentStatus status, PaymentRequest req) {
    var resp = new PaymentResponse();
    resp.setId(id);
    resp.setStatus(status);
    resp.setCardNumberLastFour(PaymentValidator.last4(req.getCardNumber()));
    resp.setExpiryMonth(req.getExpiryMonth());
    resp.setExpiryYear(req.getExpiryYear());
    resp.setCurrency(req.getCurrency());
    resp.setAmount(req.getAmount());
    return resp;
  }
}
