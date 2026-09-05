package com.checkout.payment.gateway.validator;

import com.checkout.payment.gateway.exception.InvalidCardException;
import com.checkout.payment.gateway.exception.PaymentServerErrorCode;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.ValidationFieldError;
import org.springframework.stereotype.Component;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;


@Component
public class CardValidator {
  public void validate(PostPaymentRequest postPaymentRequest) {
    List<ValidationFieldError> errors = new ArrayList<>();

    if (isCardExpired(postPaymentRequest))
      errors.add(new ValidationFieldError("expiryDate", "The card expiration date must be in the future"));

    if (!errors.isEmpty()) {
      throw new InvalidCardException(
          PaymentServerErrorCode.FIELD_VALIDATION_ERROR.errorCode,
          PaymentServerErrorCode.FIELD_VALIDATION_ERROR.errorMessage,
          errors);
    }
  }

  /*
   Check if the expiry year and month is before current year and month
   return false if the payment is expired
  */
  private boolean isCardExpired(PostPaymentRequest postPaymentRequest) {
    Integer expiryYear = postPaymentRequest.getExpiryYear();
    Integer expiryMonth = postPaymentRequest.getExpiryMonth();

    if (expiryYear == null || expiryMonth == null) {
      return true;
    }
    try {
      YearMonth expiryDate = YearMonth.of(expiryYear, expiryMonth);
      return expiryDate.isBefore(YearMonth.now());
    } catch (Exception e) {
      return true;
    }
  }
}
