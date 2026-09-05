package com.checkout.payment.gateway.exception;

import org.springframework.http.HttpStatus;

public enum PaymentServerErrorCode {
    INVALID_JSON(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Malformed JSON request. Please check your JSON syntax."),
    FIELD_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "FIELD_VALIDATION_ERROR", "Invalid request"),
    ACQUIRE_BANK_DECLINED(HttpStatus.BAD_REQUEST, "ACQUIRE_BANK_DECLINED",  "Acquiring bank declined"),
    ACQUIRE_BANK_ENDPOINT_ERROR(HttpStatus.BAD_GATEWAY, "ACQUIRE_BANK_ENDPOINT_ERROR",  "Acquiring bank service error"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND",  "Payment not found"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error");

    public final HttpStatus status;
    public final String errorCode;
    public final String errorMessage;

  PaymentServerErrorCode(HttpStatus status, String errorCode, String errorMessage) {
      this.status = status;
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
    }
}
