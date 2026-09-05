package com.checkout.payment.gateway.service.acquiringBank;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.acquireBank.BankPaymentResponseDTO;

public interface AcquireBankService {
    public BankPaymentResponseDTO submitPayment(PostPaymentRequest payment);
}
