package com.checkout.payment.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.FieldError;

@Schema(hidden = true)
public record ValidationFieldError(
    String field,
    String message
) {
  public static ValidationFieldError of(FieldError field) {
    return new ValidationFieldError(field.getField(), field.getDefaultMessage());
  }
}
