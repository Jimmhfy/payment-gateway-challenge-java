package com.checkout.payment.gateway.service.acquiringBank;

import com.checkout.payment.gateway.exception.AcquireBankEndpointException;
import com.checkout.payment.gateway.exception.PaymentServerErrorCode;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.acquireBank.BankPaymentRequestDTO;
import com.checkout.payment.gateway.model.acquireBank.BankPaymentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static com.checkout.payment.gateway.PaymentTestDataHelper.*;

@ExtendWith(MockitoExtension.class)
class DefaultAcquireBankServiceTest {
  private PostPaymentRequest request;

  @Mock
  private RestTemplate restTemplate;

  DefaultAcquireBankService acquireBankService;

  @BeforeEach
  void setUp() {
    request = buildPaymentRequest();
    acquireBankService = new DefaultAcquireBankService(restTemplate, ACQUIRING_BANK_URL);
  }

  @Test
  void whenBankAuthorizeThePaymentThenAuthorizedStatusIsReturned() {
    BankPaymentResponseDTO expectedBankResponse = buildBankPaymentResponseDTO(true);

    when(restTemplate.exchange(ACQUIRING_BANK_PAYMENT_URL,
        HttpMethod.POST,
        new HttpEntity<>(BankPaymentRequestDTO.of(request)),
        BankPaymentResponseDTO.class)
    ).thenReturn(new ResponseEntity<>(expectedBankResponse, HttpStatus.OK));

    BankPaymentResponseDTO actualBankResponse = acquireBankService.submitPayment(request);

    assertEquals(expectedBankResponse.authorized(), actualBankResponse.authorized());
  }

  @Test
  void whenBankDeclineThePaymentThenDeclinedStatusIsReturned() {
    request.setCardNumber(UNAUTHORIZED_CARD_NUMBER);
    BankPaymentResponseDTO expectedBankResponse = buildBankPaymentResponseDTO(false);

    when(restTemplate.exchange(ACQUIRING_BANK_PAYMENT_URL,
        HttpMethod.POST,
        new HttpEntity<>(BankPaymentRequestDTO.of(request)),
        BankPaymentResponseDTO.class)
    ).thenReturn(new ResponseEntity<>(expectedBankResponse, HttpStatus.OK));

    BankPaymentResponseDTO actualBankResponse = acquireBankService.submitPayment(request);

    assertEquals(expectedBankResponse.authorized(), actualBankResponse.authorized());
  }

  @Test
  void whenBankIsUnreachableThenAcquireBankEndpointExceptionIsThrown() {
    BankPaymentResponseDTO bankResponse = buildBankPaymentResponseDTO(false);

    String errorMessage = "\" "+ACQUIRING_BANK_PAYMENT_URL+" \": Connection refused";

    when(restTemplate.exchange(ACQUIRING_BANK_PAYMENT_URL,
        HttpMethod.POST,
        new HttpEntity<>(BankPaymentRequestDTO.of(request)),
        BankPaymentResponseDTO.class)
    ).thenThrow(new ResourceAccessException(errorMessage));

    AcquireBankEndpointException ex = assertThrows(AcquireBankEndpointException.class, () -> acquireBankService.submitPayment(request));
    assertEquals(PaymentServerErrorCode.ACQUIRE_BANK_ENDPOINT_ERROR, ex.getPaymentServerErrorCode());
    assertEquals(errorMessage, ex.getMessage());
  }

  @Test
  void whenBankReturns503ThenAcquireBankEndpointExceptionIsThrown() {
    request.setCardNumber(SERVICE_UNAVAILABLE_CARD_NUMBER);
    when(restTemplate.exchange(ACQUIRING_BANK_PAYMENT_URL,
        HttpMethod.POST,
        new HttpEntity<>(BankPaymentRequestDTO.of(request)),
        BankPaymentResponseDTO.class)
    ).thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    AcquireBankEndpointException ex = assertThrows(AcquireBankEndpointException.class, () -> acquireBankService.submitPayment(request));
    assertEquals(PaymentServerErrorCode.ACQUIRE_BANK_ENDPOINT_ERROR, ex.getPaymentServerErrorCode());
  }
}