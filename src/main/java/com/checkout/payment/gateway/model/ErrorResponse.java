package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "API Error Response")
public class ErrorResponse {
  @Schema(description = "Error code", example = "ACQUIRE_BANK_DECLINED",
      allowableValues = {"FIELD_VALIDATION_ERROR", "ACQUIRE_BANK_DECLINED", "ACQUIRE_BANK_ENDPOINT_ERROR", "PAYMENT_NOT_FOUND", "INTERNAL_ERROR"})
  private final String code;

  @Schema(description = "Error message", example = "Payment not found")
  private final String message;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Schema(description = "Field validation errors (present only if FIELD_VALIDATION_ERROR)")
  private final List<ValidationFieldError> errors;

  public ErrorResponse(String code, String message) {
    this(code, message, new ArrayList<>());
  }

  public ErrorResponse(String code, String message, List<ValidationFieldError> errors) {
    this.code = code;
    this.message = message;
    this.errors = errors;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public List<ValidationFieldError> getErrors() {
    return errors;
  }

  @Override
  public String toString() {
    return "ErrorResponse{" +
        "code='" + code + '\'' +
        "message='" + message + '\'' +
        "errors='" + errors + '\'' +
        '}';
  }
}
