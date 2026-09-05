package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ValidationFieldError;
import java.util.List;

public class InvalidCardException extends RuntimeException{
  private final String code;
  private final List<ValidationFieldError> errors;

  public InvalidCardException(String code, String message, List<ValidationFieldError> errors) {
    super(message);
    this.code = code;
    this.errors = errors;
  }

  public String getCode() {
    return code;
  }

  public List<ValidationFieldError> getErrors() {
    return errors;
  }
}
