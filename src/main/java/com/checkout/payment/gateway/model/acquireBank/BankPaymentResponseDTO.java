package com.checkout.payment.gateway.model.acquireBank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankPaymentResponseDTO(
    boolean authorized,
    @JsonProperty("authorization_code")
    String authCode
) {}
