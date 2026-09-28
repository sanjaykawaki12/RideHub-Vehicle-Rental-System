package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.BookingRequest;
import com.rental.entity.Booking;
import com.rental.security.AuthPrincipal;
import com.rental.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api")
public class BookingController {
    private final BookingService bookingService;
    public BookingController(BookingService bookingService){this.bookingService=bookingService;}

    @PostMapping("/user/bookings")
    public ResponseEntity<ApiResponse<Booking>> create(@AuthenticationPrincipal AuthPrincipal p,@Valid @RequestBody BookingRequest r){
        return ResponseEntity.ok(ApiResponse.ok("Booking created, proceed to payment",bookingService.createBooking(p.id(),r)));
    }
    @GetMapping("/user/bookings")
    public ResponseEntity<ApiResponse<List<Booking>>> myBookings(@AuthenticationPrincipal AuthPrincipal p){return ResponseEntity.ok(ApiResponse.ok("Bookings fetched",bookingService.getUserBookings(p.id())));}
    @GetMapping("/owner/shops/{shopId}/bookings")
    public ResponseEntity<ApiResponse<List<Booking>>> shopBookings(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long shopId){return ResponseEntity.ok(ApiResponse.ok("Bookings fetched",bookingService.getOwnerShopBookings(p.id(),shopId)));}
    @PatchMapping("/user/bookings/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancel(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long bookingId){return ResponseEntity.ok(ApiResponse.ok("Booking cancelled",bookingService.cancelBooking(p.id(),bookingId)));}
    @PatchMapping("/owner/bookings/{bookingId}/approve")
    public ResponseEntity<ApiResponse<Booking>> approve(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long bookingId){return ResponseEntity.ok(ApiResponse.ok("Booking approved",bookingService.approveBooking(p.id(),bookingId)));}
    @PatchMapping("/owner/bookings/{bookingId}/reject")
    public ResponseEntity<ApiResponse<Booking>> reject(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long bookingId){return ResponseEntity.ok(ApiResponse.ok("Booking rejected",bookingService.rejectBooking(p.id(),bookingId)));}
    @PatchMapping("/owner/bookings/{bookingId}/complete")
    public ResponseEntity<ApiResponse<Booking>> complete(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long bookingId){return ResponseEntity.ok(ApiResponse.ok("Booking marked as completed",bookingService.markCompleted(p.id(),bookingId)));}
}
