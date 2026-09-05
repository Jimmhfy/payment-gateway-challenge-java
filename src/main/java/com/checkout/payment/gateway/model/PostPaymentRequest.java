package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

@Schema(description = "Payment Request")
public class PostPaymentRequest implements Serializable {

  @Schema(description = "Full card number (14-19 digits)", example = "4321432143214321")
  @JsonProperty("card_number")
  @NotBlank(message = "Card number is mandatory")
  @Pattern(regexp = "^\\d{14,19}$", message = "Card number must be in 14 to 19 digits long")
  private String cardNumber;

  @Schema(description = "Expiry Month (1-12)", example = "1")
  @JsonProperty("expiry_month")
  @NotNull(message = "Expiry month is mandatory")
  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  private Integer expiryMonth;

  @Schema(description = "Expiry Year (YYYY)", example = "2027")
  @JsonProperty("expiry_year")
  @NotNull(message = "Expiry year is mandatory")
  @Digits(integer = 4, fraction = 0, message = "Year must be exactly 4 digits")
  private Integer expiryYear;

  @Schema(description = "Currency code in ISO 4217 format", example = "GBP", allowableValues = {"GBP", "EUR", "USD"})
  @NotBlank(message = "Currency is mandatory")
  @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be exactly 3 uppercase letters")
  @Pattern(regexp = "GBP|EUR|USD", message = "Currency code must be in one of the following: GBP, EUR, USD")
  private String currency;

  @Schema(description = "Amount in minor currency unit", example = "100")
  @NotNull(message = "Amount is mandatory")
  @Positive(message = "Payment amount must be positive")
  private Integer amount;

  @Schema(description = "Card verification value (3–4 digits)", example = "111")
  @NotBlank(message = "Card verification value CVV is mandatory")
  @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3 or 4 digits")
  private String cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
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

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  @JsonProperty("expiry_date")
  @Schema(hidden = true)
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumberLastFour=" + maskCardNumber(cardNumber) +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv=" + "[PROTECTED]" +
        '}';
  }

  /*
    Mask the card number except the last 4 digits
    for example, mask the card number 1234123412341234 to ************1234
   */
  private String maskCardNumber(String cardNumber) {
    if (cardNumber == null) return "";
    return cardNumber.replaceAll(".(?=.{4})", "*");
  }
}
