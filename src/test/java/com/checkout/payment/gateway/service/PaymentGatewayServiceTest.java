package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquireBankEndpointException;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.exception.PaymentServerErrorCode;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.acquireBank.BankPaymentResponseDTO;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.acquiringBank.AcquireBankService;
import com.checkout.payment.gateway.validator.CardValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {
  private final String AUTHORIZED_CARD_NUMBER = "4321432143214321";
  private final String UNAUTHORIZED_CARD_NUMBER = "4321432143214322";
  private final String SERVICE_UNAVAILABLE_CARD_NUMBER = "4321432143214320";

  private final String EMPTY_IDEMPOTENCY_KEY = "";
  private final String IDEMPOTENCY_KEY = "test-key-12345";

  PaymentGatewayService paymentGatewayService;

  @Mock
  PaymentsRepository paymentsRepository;

  @Mock
  AcquireBankService acquireBankService;

  CardValidator cardValidator;
  private PostPaymentRequest request;

  @BeforeEach
  void setUp() {
    cardValidator = new CardValidator();
    paymentGatewayService = new PaymentGatewayService(paymentsRepository, acquireBankService, cardValidator);

    request = buildPaymentRequest();
  }

  @Test
  public void whenPaymentIsPresentInRepositoryThenAccepted(){
    UUID paymentId = UUID.randomUUID();
    PostPaymentResponse response = buildPaymentResponse(paymentId);

    when(paymentsRepository.get(paymentId)).thenReturn(Optional.of(response));
    assertEquals(response, paymentGatewayService.getPaymentById(paymentId));
  }

  @Test
  public void whenPaymentIsNotPresentInRepositoryThenRejected(){
    UUID paymentId = UUID.randomUUID();

    when(paymentsRepository.get(paymentId)).thenReturn(Optional.empty());
    assertThrows(PaymentNotFoundException.class, () -> paymentGatewayService.getPaymentById(paymentId));
  }

  @Test
  void whenPaymentIsAuthorizedAndSavedInRepositoryThenReturnAuthorized() {
    BankPaymentResponseDTO expectedBankResponse = buildBankPaymentResponseDTO(true);
    when(acquireBankService.submitPayment(request)).thenReturn(expectedBankResponse);

    PostPaymentResponse response = paymentGatewayService.processPayment(EMPTY_IDEMPOTENCY_KEY, request);

    assertNotNull(response.getId());
    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
    assertEquals(4321, response.getCardNumberLastFour());
    assertEquals("GBP", response.getCurrency());
    assertEquals(100, response.getAmount());
    verify(paymentsRepository).add(response);
  }

  @Test
  void whenPaymentIsDeclinedAndSavedInRepositoryThenReturnDeclined() {
    request.setCardNumber(UNAUTHORIZED_CARD_NUMBER);
    BankPaymentResponseDTO expectedBankResponse = buildBankPaymentResponseDTO(false);
    when(acquireBankService.submitPayment(request)).thenReturn(expectedBankResponse);

    PostPaymentResponse response = paymentGatewayService.processPayment(EMPTY_IDEMPOTENCY_KEY, request);

    assertNotNull(response.getId());
    assertEquals(PaymentStatus.DECLINED, response.getStatus());
    verify(paymentsRepository).add(response);
  }

  @Test
  void whenPaymentProcessedAndNoAcquiringBankRequestCalled() {
    UUID paymentId = UUID.randomUUID();
    PostPaymentResponse expectedResponse = buildPaymentResponse(paymentId);

    when(paymentsRepository.getIdByIdempotentKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(paymentId));
    when(paymentsRepository.get(paymentId)).thenReturn(Optional.of(expectedResponse));

    PostPaymentResponse actualResponse = paymentGatewayService.processPayment(IDEMPOTENCY_KEY, request);

    assertNotNull(actualResponse.getId());
    assertEquals(expectedResponse, actualResponse);

    verify(acquireBankService, never()).submitPayment(request);
  }

  @Test
  void whenBankIsUnavailableThenNothingIsSaved() {
    request.setCardNumber(SERVICE_UNAVAILABLE_CARD_NUMBER);
    when(acquireBankService.submitPayment(request))
        .thenThrow(new AcquireBankEndpointException(PaymentServerErrorCode.ACQUIRE_BANK_ENDPOINT_ERROR));

    assertThrows(AcquireBankEndpointException.class, () -> paymentGatewayService.processPayment(EMPTY_IDEMPOTENCY_KEY, request));

    verify(paymentsRepository, never()).add(any());
  }

  private PostPaymentRequest buildPaymentRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber(AUTHORIZED_CARD_NUMBER);
    request.setAmount(100);
    request.setCurrency("GBP");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    return request;
  }

  private PostPaymentResponse buildPaymentResponse(UUID paymentId) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(paymentId);
    response.setAmount(100);
    response.setCurrency("GBP");
    response.setStatus(PaymentStatus.AUTHORIZED);
    response.setExpiryMonth(12);
    response.setExpiryYear(2027);
    response.setCardNumberLastFour(4321);
    return response;
  }

  private BankPaymentResponseDTO buildBankPaymentResponseDTO(boolean authorized) {
    final String AUTHORIZED_CODE = "f973e576-c898-448a-800d-9effc0e064e4";
    return new BankPaymentResponseDTO(authorized, authorized ? AUTHORIZED_CODE : "");
  }
}