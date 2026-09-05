package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.acquireBank.BankPaymentResponseDTO;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import com.checkout.payment.gateway.service.acquiringBank.AcquireBankService;
import com.checkout.payment.gateway.validator.CardValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);
  private final PaymentsRepository paymentsRepository;
  private final AcquireBankService acquireBankService;
  private final CardValidator cardValidator;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, AcquireBankService acquireBankService, CardValidator cardValidator) {
    this.paymentsRepository = paymentsRepository;
    this.acquireBankService = acquireBankService;
    this.cardValidator = cardValidator;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new PaymentNotFoundException(String.format("Payment %s not found", id)));
  }

  public PostPaymentResponse processPayment(String idempotencyKey, PostPaymentRequest paymentRequest) {
    cardValidator.validate(paymentRequest);

    // Check if the payment already processed
    PostPaymentResponse processedPayment = getProcessedPayment(idempotencyKey);
    if (processedPayment != null) {
      LOG.info("Payment already processed. idempotencyKey={}", idempotencyKey);
      return processedPayment;
    }

    // Send payment detail to acquiring bank
    BankPaymentResponseDTO bankResponse = acquireBankService.submitPayment(paymentRequest);

    PaymentStatus paymentStatus = bankResponse.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
    UUID paymentId = UUID.randomUUID();
    PostPaymentResponse postPaymentResponse = new PostPaymentResponse(paymentId, paymentRequest, paymentStatus);

    savePaymentResponse(postPaymentResponse, paymentId, idempotencyKey);

    return postPaymentResponse;
  }

  // Transactional on saving repository
  private void savePaymentResponse(PostPaymentResponse postPaymentResponse, UUID paymentId, String idempotencyKey) {
    // Store the payment result to repository
    paymentsRepository.add(postPaymentResponse);

    // Store the idempotencyKey paymentID pair if presented.
    if (idempotencyKey != null && !idempotencyKey.isBlank())
      paymentsRepository.addIdempotentKey(idempotencyKey, paymentId);
  }

  private PostPaymentResponse getProcessedPayment(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank())
      return null;

    return paymentsRepository.getIdByIdempotentKey(idempotencyKey)
        .flatMap(paymentsRepository::get)
        .orElse(null);
  }
}
