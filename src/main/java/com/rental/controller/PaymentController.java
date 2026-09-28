package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.CreateOrderRequest;
import com.rental.dto.VerifyPaymentRequest;
import com.rental.entity.Payment;
import com.rental.security.AuthPrincipal;
import com.rental.service.PaymentService;
import jakarta.validation.Valid;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/user/payments")
public class PaymentController {
    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService){this.paymentService=paymentService;}
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Object>> createOrder(@AuthenticationPrincipal AuthPrincipal p,@Valid @RequestBody CreateOrderRequest r) throws Exception {
        JSONObject order=paymentService.createOrder(r.getBookingId(),p.id());
        return ResponseEntity.ok(ApiResponse.ok("Payment order created",order.toMap()));
    }
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Payment>> verify(@AuthenticationPrincipal AuthPrincipal p,@Valid @RequestBody VerifyPaymentRequest r){
        return ResponseEntity.ok(ApiResponse.ok("Payment verified, waiting for owner approval",paymentService.verifyAndCapture(r,p.id())));
    }
}
