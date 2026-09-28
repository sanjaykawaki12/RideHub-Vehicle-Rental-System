package com.rental.service.impl;

import com.rental.dto.VerifyPaymentRequest;
import com.rental.entity.Booking;
import com.rental.entity.Payment;
import com.rental.entity.enums.PaymentStatus;
import com.rental.exception.ApiException;
import com.rental.repository.PaymentRepository;
import com.rental.repository.NotificationRepository;
import com.rental.service.BookingService;
import com.rental.service.PaymentService;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * TESTING ONLY. Active when spring.profiles.active includes "mock-payment".
 * Simulates a Razorpay order + successful payment without calling Razorpay,
 * so you can test the full booking -> payment -> confirmed flow with
 * Postman/curl before your Razorpay KYC/keys are ready.
 *
 * Switch to real Razorpay by removing "mock-payment" from
 * spring.profiles.active in application.properties.
 */
@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final NotificationRepository notificationRepository;

    public MockPaymentServiceImpl(PaymentRepository paymentRepository, BookingService bookingService, NotificationRepository notificationRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public JSONObject createOrder(Long bookingId, Long userId) {
        Booking booking = bookingService.getByIdOrThrow(bookingId);
        if (!booking.getUser().getId().equals(userId)) throw new ApiException("You do not have access to this booking", HttpStatus.FORBIDDEN);
        if (booking.getStatus() != com.rental.entity.enums.BookingStatus.PENDING_PAYMENT) throw new ApiException("Booking is not awaiting payment", HttpStatus.BAD_REQUEST);
        Payment existing = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (existing != null && existing.getStatus() == PaymentStatus.CREATED) {
            JSONObject existingResponse = new JSONObject();
            existingResponse.put("orderId", existing.getRazorpayOrderId());
            existingResponse.put("amount", booking.getTotalAmount().multiply(java.math.BigDecimal.valueOf(100)).intValue());
            existingResponse.put("currency", "INR");
            existingResponse.put("bookingId", booking.getId());
            existingResponse.put("keyId", "mock_key_id");
            return existingResponse;
        }

        String mockOrderId = "mock_order_" + UUID.randomUUID();

        Payment payment = Payment.builder()
                .booking(booking)
                .razorpayOrderId(mockOrderId)
                .amount(booking.getTotalAmount())
                .status(PaymentStatus.CREATED)
                .build();
        paymentRepository.save(payment);

        JSONObject response = new JSONObject();
        response.put("orderId", mockOrderId);
        response.put("amount", booking.getTotalAmount().multiply(java.math.BigDecimal.valueOf(100)).intValue());
        response.put("currency", "INR");
        response.put("keyId", "mock_key_id");
        response.put("bookingId", booking.getId());
        response.put("note", "MOCK MODE - no real payment gateway called. Use this orderId to call /verify.");
        return response;
    }

    @Override
    public Payment verifyAndCapture(VerifyPaymentRequest request, Long userId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ApiException("Payment record not found. Call create-order first.", HttpStatus.NOT_FOUND));
        if (!payment.getBooking().getUser().getId().equals(userId)) throw new ApiException("You do not have access to this payment", HttpStatus.FORBIDDEN);

        // Mock mode: skip real signature verification, always mark success
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        bookingService.markConfirmed(payment.getBooking().getId());
        return payment;
    }
}
