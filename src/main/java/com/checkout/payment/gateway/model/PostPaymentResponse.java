package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Response of payment details")
public class PostPaymentResponse {
  @Schema(description = "UUID", example = "c85c3e6a-a247-4270-abc8-3cb2bc0b66d1")
  private UUID id;

  @Schema(description = "Payment status", example = "Authorized")
  private PaymentStatus status;

  @Schema(description = "Last four digits of card number", example = "4321")
  private Integer cardNumberLastFour;

  @Schema(description = "Expiry Month (1-12)", example = "1")
  private Integer expiryMonth;

  @Schema(description = "Expiry Year (YYYY)", example = "2027")
  private Integer expiryYear;

  @Schema(description = "Currency code in ISO 4217 format", example = "GBP", allowableValues = {"GBP", "EUR", "USD"})
  private String currency;

  @Schema(description = "Amount in minor currency unit", example = "100")
  private Integer amount;

  public PostPaymentResponse () {}

  public PostPaymentResponse(UUID id, PostPaymentRequest request, PaymentStatus status) {
    this.id = id;
    this.status = status;
    this.cardNumberLastFour = getLastFourDigits(request.getCardNumber());
    this.expiryMonth = request.getExpiryMonth();
    this.expiryYear = request.getExpiryYear();
    this.currency = request.getCurrency();
    this.amount = request.getAmount();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentStatus status) {
    this.status = status;
  }

  public Integer getCardNumberLastFour() {
    return cardNumberLastFour;
  }

  public void setCardNumberLastFour(int cardNumberLastFour) {
    this.cardNumberLastFour = cardNumberLastFour;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(int expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(int expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  @Override
  public String toString() {
    return "PostPaymentResponse{" +
        "id=" + id +
        ", status=" + status +
        ", cardNumberLastFour=" + cardNumberLastFour +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }

  private int getLastFourDigits(String cardNumber) {
    return Integer.parseInt(cardNumber.substring(cardNumber.length() - 4));
  }
}
