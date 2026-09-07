package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.ValidationFieldError;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.List;

@ControllerAdvice
public class CommonExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  private final MeterRegistry meterRegistry;

  public CommonExceptionHandler(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleException(PaymentNotFoundException ex) {
    LOG.error("Payment not found exception, msg={}", ex.getMessage());
    return new ResponseEntity<>(
        new ErrorResponse(
            PaymentServerErrorCode.PAYMENT_NOT_FOUND.errorCode,
            ex.getMessage()
        ), PaymentServerErrorCode.PAYMENT_NOT_FOUND.status);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

    // Load all validation error into one list
    String errorMessage = PaymentServerErrorCode.FIELD_VALIDATION_ERROR.errorMessage;

    // Convert String validation error to our ValidationFieldError for necessary field and message
    List<ValidationFieldError> validationFieldErrors = ex.getBindingResult().getFieldErrors().stream().map(ValidationFieldError::of).toList();

    LOG.error("Field validation error, msg={}", errorMessage);
    return new ResponseEntity<>(
        new ErrorResponse(
            PaymentServerErrorCode.FIELD_VALIDATION_ERROR.errorCode,
            errorMessage,
            validationFieldErrors
        ), PaymentServerErrorCode.FIELD_VALIDATION_ERROR.status);
  }

  @ExceptionHandler(InvalidCardException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(InvalidCardException ex) {

    // Load all validation error into one list
    String errorMessage = ex.getMessage();

    LOG.error("Card detail validation error, msg={}", errorMessage);
    return new ResponseEntity<>(
        new ErrorResponse(
            ex.getCode(),
            errorMessage,
            ex.getErrors()
        ), PaymentServerErrorCode.FIELD_VALIDATION_ERROR.status);
  }

  @ExceptionHandler(AcquireBankEndpointException.class)
  public ResponseEntity<ErrorResponse> handleAcquireBankEndpointException(AcquireBankEndpointException ex) {
    String errorMessage = ex.getMessage();
    LOG.error("Acquire bank endpoint return error, msg={}", errorMessage);
    meterRegistry.counter("acquiring.bank.endpoint.errors.total", "exception", ex.getClass().getSimpleName()).increment();
    return new ResponseEntity<>(
        new ErrorResponse(
            ex.getPaymentServerErrorCode().errorCode,
            errorMessage
        ), ex.getPaymentServerErrorCode().status);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    LOG.error("Invalid JSON, cause={}, msg={}", ex.getCause(), ex.getMessage());
    return new ResponseEntity<>(
        new ErrorResponse(
            PaymentServerErrorCode.INVALID_JSON.errorCode,
            PaymentServerErrorCode.INVALID_JSON.errorMessage
        ), PaymentServerErrorCode.INVALID_JSON.status);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
    LOG.error("Unexpected error, msg={}", ex.getMessage());
    meterRegistry.counter("uncaught.errors.total", "exception", ex.getClass().getSimpleName()).increment();
    return new ResponseEntity<>(
        new ErrorResponse(
            PaymentServerErrorCode.INTERNAL_ERROR.errorCode,
            PaymentServerErrorCode.INTERNAL_ERROR.errorMessage
        ), PaymentServerErrorCode.INTERNAL_ERROR.status);
  }
}
