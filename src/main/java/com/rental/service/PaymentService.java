package com.rental.service;

import com.rental.dto.VerifyPaymentRequest;
import com.rental.entity.Payment;
import org.json.JSONObject;

public interface PaymentService {
    JSONObject createOrder(Long bookingId, Long userId) throws Exception;
    Payment verifyAndCapture(VerifyPaymentRequest request, Long userId);
}
