package com.checkout.payment.gateway.exception;

import org.springframework.validation.FieldError;
import java.util.ArrayList;
import java.util.List;

public class AcquireBankEndpointException extends RuntimeException {
  private final PaymentServerErrorCode paymentServerErrorCode;
  private final String message;

  public AcquireBankEndpointException(PaymentServerErrorCode paymentServerErrorCode) {
    this(paymentServerErrorCode.errorMessage, paymentServerErrorCode);
  }

  public AcquireBankEndpointException(String message, PaymentServerErrorCode paymentServerErrorCode) {
    super(message);
    this.message = message;
    this.paymentServerErrorCode = paymentServerErrorCode;
  }

  public PaymentServerErrorCode getPaymentServerErrorCode() {
    return paymentServerErrorCode;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
