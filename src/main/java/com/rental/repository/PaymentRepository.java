package com.rental.repository;
import com.rental.entity.Payment;
import com.rental.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment,Long>{ Optional<Payment> findByRazorpayOrderId(String id); Optional<Payment> findByBookingId(Long bookingId); long countByStatus(PaymentStatus status); }
