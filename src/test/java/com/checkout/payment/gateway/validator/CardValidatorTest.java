package com.checkout.payment.gateway.cardValidator;

import com.checkout.payment.gateway.exception.InvalidCardException;
import com.checkout.payment.gateway.exception.PaymentServerErrorCode;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.ValidationFieldError;
import com.checkout.payment.gateway.validator.CardValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class CardValidatorTest {
  private CardValidator cardValidator;
  private PostPaymentRequest request;

  @BeforeEach
  void setUp() {
    cardValidator = new CardValidator();
    request = buildPaymentRequest();
  }

  @Test
  void whenExpiryDateIsInTheFutureThenAccepted() {
    assertDoesNotThrow(() -> cardValidator.validate(request));
  }

  @Test
  void whenExpiryDateIsInCurrentMonthYearThenAccepted() {
    YearMonth current = YearMonth.now();
    request.setExpiryMonth(current.getMonthValue());
    request.setExpiryYear(current.getYear());

    assertDoesNotThrow(() -> cardValidator.validate(request));
  }

  @Test
  void whenExpiryDateIsInThePastThenRejected() {
    YearMonth past = YearMonth.now().minusMonths(1);
    request.setExpiryMonth(past.getMonthValue());
    request.setExpiryYear(past.getYear());

    InvalidCardException ex = assertThrows(InvalidCardException.class, () -> cardValidator.validate(request));

    assertEquals(PaymentServerErrorCode.FIELD_VALIDATION_ERROR.errorCode, ex.getCode());

    ValidationFieldError error = ex.getErrors().stream().findFirst().orElseThrow();
    assertEquals("expiryDate", error.field());
    assertEquals("The card expiration date must be in the future", error.message());
  }

  @Test
  void whenExpiryDateIsAbsentThenRejected() {
    request.setExpiryMonth(null);
    request.setExpiryYear(null);

    InvalidCardException ex = assertThrows(InvalidCardException.class, () -> cardValidator.validate(request));

    assertEquals(PaymentServerErrorCode.FIELD_VALIDATION_ERROR.errorCode, ex.getCode());

    ValidationFieldError error = ex.getErrors().stream().findFirst().orElseThrow();
    assertEquals("expiryDate", error.field());
    assertEquals("The card expiration date must be in the future", error.message());
  }

  private PostPaymentRequest buildPaymentRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    YearMonth future = YearMonth.now().plusYears(1);
    request.setExpiryYear(future.getYear());
    request.setExpiryMonth(future.getMonthValue());
    return request;
  }
}