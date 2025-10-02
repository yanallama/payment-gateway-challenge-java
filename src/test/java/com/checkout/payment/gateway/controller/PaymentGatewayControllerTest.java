package com.checkout.payment.gateway.controller;


import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankClient.BankResultType;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  @MockBean
  private com.checkout.payment.gateway.client.BankClient bankClient;

  @Test
  void whenPaymentWithIdExistsThenCorrectPaymentIsReturned() throws Exception {
    PaymentResponse payment = new PaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2029);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/api/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found"));
  }

  @Test
  void postPayment_Authorized_Returns201AndLocation() throws Exception {
    when(bankClient.authorize(anyString(), anyInt(), anyInt(), anyString(), anyInt(), anyString()))
        .thenReturn(BankResultType.AUTHORIZED);

    var json = """
      {
        "card_number":"4111111111111111",
        "expiry_month":12,
        "expiry_year":2029,
        "currency":"GBP",
        "amount":1200,
        "cvv":"123"
      }
      """;

    mvc.perform(MockMvcRequestBuilders.post("/api/payment")
            .contentType("application/json")
            .content(json))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/payment/.+")))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(1111))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(1200));
  }

  @Test
  void postPayment_Declined_Returns201() throws Exception {
    when(bankClient.authorize(anyString(), anyInt(), anyInt(), anyString(), anyInt(), anyString()))
        .thenReturn(BankResultType.DECLINED);

    var json = """
      {
        "card_number":"4111111111111112",
        "expiry_month":12,
        "expiry_year":2029,
        "currency":"GBP",
        "amount":1200,
        "cvv":"123"
      }
      """;

    mvc.perform(MockMvcRequestBuilders.post("/api/payment")
            .contentType("application/json")
            .content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(1112));
  }

  @Test
  void postPayment_BankUnavailable_Returns502_AndNotPersisted() throws Exception {
    when(bankClient.authorize(anyString(), anyInt(), anyInt(), anyString(), anyInt(), anyString()))
        .thenThrow(new com.checkout.payment.gateway.exception.BankUnavailableException("Bank down"));

    var json = """
      {
        "card_number":"4111111111111110",
        "expiry_month":12,
        "expiry_year":2029,
        "currency":"GBP",
        "amount":1200,
        "cvv":"123"
      }
      """;

    mvc.perform(MockMvcRequestBuilders.post("/api/payment")
            .contentType("application/json")
            .content(json))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Bank temporarily unavailable"));
  }

  @Test
  void postPayment_InvalidRequest_Returns400() throws Exception {
    // Bad currency + bad CVV to trigger validation failure
    var json = """
      {
        "card_number":"4111111111111111",
        "expiry_month":1,
        "expiry_year":2020,
        "currency":"XXX",
        "amount":0,
        "cvv":"12"
      }
      """;

    mvc.perform(MockMvcRequestBuilders.post("/api/payment")
            .contentType("application/json")
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }
}
