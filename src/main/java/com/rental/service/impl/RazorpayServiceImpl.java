package com.rental.service.impl;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Creates a Razorpay order for a booking and verifies the payment signature
 * sent back by Razorpay Checkout after a successful transaction.
 * Fill razorpay.key.id / razorpay.key.secret in application.properties
 * with the keys from your Razorpay dashboard (Settings -> API Keys).
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.payment.provider", havingValue = "razorpay", matchIfMissing = false)
public class RazorpayServiceImpl implements PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final NotificationRepository notificationRepository;

    public RazorpayServiceImpl(PaymentRepository paymentRepository, BookingService bookingService, NotificationRepository notificationRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public JSONObject createOrder(Long bookingId, Long userId) throws Exception {
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
            existingResponse.put("keyId", keyId);
            return existingResponse;
        }

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        // Razorpay expects amount in the smallest currency unit (paise for INR)
        orderRequest.put("amount", booking.getTotalAmount().multiply(java.math.BigDecimal.valueOf(100)).intValue());
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "booking_" + booking.getId());

        com.razorpay.Order order = client.orders.create(orderRequest);

        Payment payment = Payment.builder()
                .booking(booking)
                .razorpayOrderId((String) order.get("id"))
                .amount(booking.getTotalAmount())
                .status(PaymentStatus.CREATED)
                .build();
        paymentRepository.save(payment);

        JSONObject response = new JSONObject();
        response.put("orderId", (String) order.get("id"));
        response.put("amount", orderRequest.get("amount"));
        response.put("currency", "INR");
        response.put("keyId", keyId); // frontend needs this to open Razorpay Checkout
        response.put("bookingId", booking.getId());
        return response;
    }

    @Override
    public Payment verifyAndCapture(VerifyPaymentRequest request, Long userId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ApiException("Payment record not found", HttpStatus.NOT_FOUND));
        if (!payment.getBooking().getUser().getId().equals(userId)) throw new ApiException("You do not have access to this payment", HttpStatus.FORBIDDEN);

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean valid = Utils.verifyPaymentSignature(options, keySecret);
            if (!valid) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new ApiException("Payment signature verification failed", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new ApiException("Payment verification error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        bookingService.markConfirmed(payment.getBooking().getId());
        return payment;
    }
}
