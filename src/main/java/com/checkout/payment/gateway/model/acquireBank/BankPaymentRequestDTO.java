package com.checkout.payment.gateway.model.acquireBank;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BankPaymentRequestDTO(
    @JsonProperty("card_number")
    String cardNumber,
    @JsonProperty("expiry_date")
    String expiryDate,
    String currency,
    String amount,
    String cvv
) {
  public static BankPaymentRequestDTO of(PostPaymentRequest request) {
    if (request == null) return null;
    return new BankPaymentRequestDTO(
        request.getCardNumber(),
        request.getExpiryDate(),
        request.getCurrency(),
        request.getAmount().toString(),
        request.getCvv());
  }
}
