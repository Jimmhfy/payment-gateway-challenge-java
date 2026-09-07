package com.checkout.payment.gateway;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.acquireBank.BankPaymentResponseDTO;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PaymentTestDataHelper {
  public final static String AUTHORIZED_CARD_NUMBER = "4321432143214321";
  public final static String UNAUTHORIZED_CARD_NUMBER = "4321432143214322";
  public final static String SERVICE_UNAVAILABLE_CARD_NUMBER = "4321432143214320";

  public final static String EMPTY_IDEMPOTENCY_KEY = "";
  public final static String IDEMPOTENCY_KEY = "test-key-12345";

  public static final String ACQUIRING_BANK_URL = "http://localhost:8080";
  public static final String ACQUIRING_BANK_PAYMENT_URL = ACQUIRING_BANK_URL + "/payments";

  private static final YearMonth inThePass = YearMonth.now().minusYears(1);
  private static final YearMonth inTheFuture = YearMonth.now().plusYears(1);

  public static Map<String, Object> buildPaymentBody() {
    Map<String, Object> bodyMap = new HashMap<>();
    bodyMap.put("card_number", AUTHORIZED_CARD_NUMBER);

    bodyMap.put("expiry_month", inTheFuture.getMonthValue());
    bodyMap.put("expiry_year", inTheFuture.getYear());

    bodyMap.put("currency", "GBP");
    bodyMap.put("amount", 100);
    bodyMap.put("cvv", "111");

    return bodyMap;
  }

  public static PostPaymentRequest buildPaymentRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber(AUTHORIZED_CARD_NUMBER);
    request.setAmount(100);
    request.setCurrency("GBP");
    request.setExpiryMonth(inTheFuture.getMonth().getValue());
    request.setExpiryYear(inTheFuture.getYear());
    return request;
  }

  public static PostPaymentResponse buildPaymentResponse(UUID paymentId) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(paymentId);
    response.setAmount(100);
    response.setCurrency("GBP");
    response.setStatus(PaymentStatus.AUTHORIZED);
    response.setExpiryMonth(inTheFuture.getMonth().getValue());
    response.setExpiryYear(inTheFuture.getYear());
    response.setCardNumberLastFour("4321");
    return response;
  }

  public static BankPaymentResponseDTO buildBankPaymentResponseDTO(boolean authorized) {
    final String AUTHORIZED_CODE = "f973e576-c898-448a-800d-9effc0e064e4";
    return new BankPaymentResponseDTO(authorized, authorized ? AUTHORIZED_CODE : "");
  }
}
