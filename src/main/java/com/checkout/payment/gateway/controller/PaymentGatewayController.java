package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.net.URI;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaymentGatewayController {

  private final PaymentGatewayService svc;

  public PaymentGatewayController(PaymentGatewayService svc) {
    this.svc = svc;
  }

  @Operation(summary = "Retrieve a previously created payment by ID")
  @GetMapping("/payment/{id}")
  public ResponseEntity<PaymentResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(svc.getPaymentById(id));
  }

  @Operation(summary = "Process a card payment via the acquiring bank")
  @PostMapping("/payment")
  public ResponseEntity<PaymentResponse> post(@RequestBody PaymentRequest req) {
    var saved = svc.processPayment(req);
    return ResponseEntity
        .created(URI.create("/api/payment/" + saved.getId()))
        .body(saved);
  }
}
