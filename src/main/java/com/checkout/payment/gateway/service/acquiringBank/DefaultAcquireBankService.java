package com.checkout.payment.gateway.service.acquiringBank;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquireBankEndpointException;
import com.checkout.payment.gateway.exception.PaymentServerErrorCode;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.acquireBank.BankPaymentRequestDTO;
import com.checkout.payment.gateway.model.acquireBank.BankPaymentResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class DefaultAcquireBankService implements AcquireBankService {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultAcquireBankService.class);

  private final RestTemplate restTemplate;
  private final String bankURL;

  public DefaultAcquireBankService(RestTemplate restTemplate, @Value("${acquiring-bank.gateway.url}") String bankURL) {
    this.restTemplate = restTemplate;
    this.bankURL = bankURL;
  }

  @Override
  public BankPaymentResponseDTO submitPayment(PostPaymentRequest payment) {
    BankPaymentRequestDTO requestDTO = BankPaymentRequestDTO.of(payment);
    String endpointURL = bankURL+"/payments";
    LOG.info("[ACQUIRING BANK] Submit payment request to acquiring bank: currency={}, amount={}", requestDTO.currency(), requestDTO.amount());
    LOG.info("[ACQUIRING BANK] Submit to URL={}", endpointURL);

    try {
      ResponseEntity<BankPaymentResponseDTO> response = restTemplate.exchange(
          endpointURL, HttpMethod.POST, new HttpEntity<>(requestDTO), BankPaymentResponseDTO.class);

      BankPaymentResponseDTO responseDTO = response.getBody();

      if (responseDTO == null) {
        throw new AcquireBankEndpointException("Acquiring bank return empty response", PaymentServerErrorCode.ACQUIRE_BANK_ENDPOINT_ERROR);
      }

      LOG.info("[ACQUIRING BANK] Payment response: authorized={}, authCode={}", responseDTO.authorized(), responseDTO.authCode());
      return responseDTO;
    } catch (HttpClientErrorException e) {
      LOG.error("[ACQUIRING BANK] Client error occurred: status_code={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
      throw new AcquireBankEndpointException(e.getMessage(), PaymentServerErrorCode.ACQUIRE_BANK_DECLINED);
    } catch (HttpServerErrorException e) {
      LOG.error("[ACQUIRING BANK] HTTP Server error occurred: status_code={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
      throw new AcquireBankEndpointException(e.getMessage(), PaymentServerErrorCode.ACQUIRE_BANK_ENDPOINT_ERROR);
    } catch (ResourceAccessException e) {
      LOG.error("[ACQUIRING BANK] Network timeout or connection error: {}", e.getMessage());
      throw new AcquireBankEndpointException(e.getMessage(), PaymentServerErrorCode.ACQUIRE_BANK_ENDPOINT_ERROR);
    }
  }
}
