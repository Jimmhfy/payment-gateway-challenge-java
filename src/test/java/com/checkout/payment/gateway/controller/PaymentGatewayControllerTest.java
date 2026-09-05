package com.checkout.payment.gateway.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquireBankEndpointException;
import com.checkout.payment.gateway.exception.PaymentServerErrorCode;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.acquireBank.BankPaymentResponseDTO;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.checkout.payment.gateway.service.acquiringBank.AcquireBankService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {
  @Autowired
  private MockMvc mvc;

  @Autowired
  private PaymentsRepository paymentsRepository;

  @MockBean
  private AcquireBankService acquireBankService;

  @Autowired
  private ObjectMapper objectMapper;

  private Map<String, Object> jsonMap;
  private final String AUTHORIZED_CARD_NUMBER = "4321432143214321";
  private final String UNAUTHORIZED_CARD_NUMBER = "4321432143214322";
  private final String SERVICE_UNAVAILABLE_CARD_NUMBER = "4321432143214320";

  @BeforeEach
  void setUp() {
    paymentsRepository.reset();
    jsonMap = buildPaymentBody();
  }

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
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
    UUID missingId = UUID.randomUUID();
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + missingId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment " + missingId + " not found"))
        .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
  }

  @Test
  void whenPaymentIsAuthorizedThen200IsReturned() throws Exception {
    BankPaymentResponseDTO expectedBankResponse = buildBankPaymentResponseDTO(true);
    when(acquireBankService.submitPayment(any())).thenReturn(expectedBankResponse);

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(jsonMap)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(4321))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.currency").value("GBP"));
  }

  @Test
  void whenPaymentIsDeclinedThen200IsReturned() throws Exception {
    jsonMap.put("card_number", UNAUTHORIZED_CARD_NUMBER);
    BankPaymentResponseDTO expectedBankResponse = buildBankPaymentResponseDTO(false);
    when(acquireBankService.submitPayment(any())).thenReturn(expectedBankResponse);

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(jsonMap)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"));
  }

  @Test
  void whenRequestBodyIsEmptyThen400IsReturnedWithRequiredFieldErrors() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("FIELD_VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[?(@.field == 'cardNumber')].message")
            .value("Card number is mandatory"))
        .andExpect(jsonPath("$.errors[?(@.field == 'expiryMonth')].message")
            .value("Expiry month is mandatory"))
        .andExpect(jsonPath("$.errors[?(@.field == 'expiryYear')].message")
            .value("Expiry year is mandatory"))
        .andExpect(jsonPath("$.errors[?(@.field == 'currency')].message")
            .value("Currency is mandatory"))
        .andExpect(jsonPath("$.errors[?(@.field == 'amount')].message")
            .value("Amount is mandatory"))
        .andExpect(jsonPath("$.errors[?(@.field == 'cvv')].message")
            .value("Card verification value CVV is mandatory"));

    verify(acquireBankService, never()).submitPayment(any());
  }

  @Test
  void whenExpiryDateIsInThePastThen400IsReturnedAndBankIsNotCalled() throws Exception {
    YearMonth past = YearMonth.now().minusYears(1);
    jsonMap.put("expiry_month", past.getMonthValue());
    jsonMap.put("expiry_year", past.getYear());

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(jsonMap)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("FIELD_VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[?(@.field == 'expiryDate')].message")
            .value("The card expiration date must be in the future"));

    verify(acquireBankService, never()).submitPayment(any());
  }

  @Test
  void whenPaymentIsRejectedThen400IsReturnedAndBankIsNotCalled() throws Exception {
    jsonMap.put("amount", 0);

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(jsonMap)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("FIELD_VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Invalid request"))
        .andExpect(jsonPath("$.errors").isArray());

    verify(acquireBankService, never()).submitPayment(any());
  }

  @Test
  void whenBankIsUnavailableThen502IsReturned() throws Exception {
    jsonMap.put("card_number", SERVICE_UNAVAILABLE_CARD_NUMBER);
    
    when(acquireBankService.submitPayment(any()))
        .thenThrow(new AcquireBankEndpointException(PaymentServerErrorCode.ACQUIRE_BANK_ENDPOINT_ERROR));

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(jsonMap)))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("ACQUIRE_BANK_ENDPOINT_ERROR"))
        .andExpect(jsonPath("$.message").value("Acquiring bank service error"));
  }

  @Test
  void whenPaymentIsPostedThenItCanBeRetrievedById() throws Exception {
    BankPaymentResponseDTO expectedBankResponse = buildBankPaymentResponseDTO(true);
    when(acquireBankService.submitPayment(any())).thenReturn(expectedBankResponse);

    String response = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(jsonMap)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String paymentId = objectMapper.readTree(response).get("id").asText();

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId))
        .andExpect(jsonPath("$.status").value("Authorized"));
  }

  private Map<String, Object> buildPaymentBody() {
    Map<String, Object> bodyMap = new HashMap<>();
    bodyMap.put("card_number", AUTHORIZED_CARD_NUMBER);

    YearMonth future = YearMonth.now().plusYears(1);
    bodyMap.put("expiry_month", future.getMonthValue());
    bodyMap.put("expiry_year", future.getYear());

    bodyMap.put("currency", "GBP");
    bodyMap.put("amount", 100);
    bodyMap.put("cvv", "111");

    return bodyMap;
  }

  private BankPaymentResponseDTO buildBankPaymentResponseDTO(boolean authorized) {
    final String AUTHORIZED_CODE = "f973e576-c898-448a-800d-9effc0e064e4";
    return new BankPaymentResponseDTO(authorized, authorized ? AUTHORIZED_CODE : "");
  }
}
