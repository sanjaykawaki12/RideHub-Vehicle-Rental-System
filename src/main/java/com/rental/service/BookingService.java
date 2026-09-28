package com.rental.service;

import com.rental.dto.BookingRequest;
import com.rental.entity.Booking;
import java.util.List;

public interface BookingService {
    Booking createBooking(Long userId, BookingRequest request);
    List<Booking> getUserBookings(Long userId);
    List<Booking> getShopBookings(Long shopId);
    List<Booking> getOwnerShopBookings(Long ownerId, Long shopId);
    Booking getByIdOrThrow(Long bookingId);
    void markConfirmed(Long bookingId);
    Booking approveBooking(Long ownerId, Long bookingId);
    Booking rejectBooking(Long ownerId, Long bookingId);
    Booking markCompleted(Long ownerId, Long bookingId);
    Booking cancelBooking(Long userId, Long bookingId);
}
