package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @Operation(
      summary = "Send payment",
      description = "Processes a payment request and returns the payment result."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Payment successful",
          content = @Content(
              schema = @Schema(implementation = PostPaymentResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Validation error",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(
                  value = """
                      {
                         "code": "FIELD_VALIDATION_ERROR",
                         "message": "Invalid request",
                         "errors": [
                           {
                             "field": "currency",
                             "message": "Currency is mandatory"
                           }
                         ]
                       }
                    """
              )
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Unexpected server error",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(
                  value = """
                    {
                      "code": "INTERNAL_ERROR",
                      "message": "Unexpected server error"
                    }
                    """
              )
          )
      ),
      @ApiResponse(
          responseCode = "502",
          description = "Acquiring bank service error",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(
                  value = """
                    {
                      "code": "ACQUIRE_BANK_ENDPOINT_ERROR",
                      "message": "Acquiring bank service error"
                    }
                    """
              )
          )
      )
  })
  @PostMapping("/payment")
  public ResponseEntity<PostPaymentResponse> processPayment(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody PostPaymentRequest postPaymentRequest) {
    return new ResponseEntity<>(paymentGatewayService.processPayment(idempotencyKey, postPaymentRequest), HttpStatus.OK);
  }

  @Operation(summary = "Retrieve processed payment")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Payment successful",
          content = @Content(
              schema = @Schema(implementation = PostPaymentResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Payment not found",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(
                  value = """
                    {
                      "code": "PAYMENT_NOT_FOUND",
                      "message": "Payment 1234 not found"
                    }
                    """
              )
          )
      )
  })
  @GetMapping("/payment/{id}")
  public ResponseEntity<PostPaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }
}
