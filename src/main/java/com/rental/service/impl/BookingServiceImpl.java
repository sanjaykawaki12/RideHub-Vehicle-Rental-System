package com.rental.service.impl;

import com.rental.dto.BookingRequest;
import com.rental.entity.*;
import com.rental.entity.enums.BookingStatus;
import com.rental.entity.enums.VehicleStatus;
import com.rental.exception.ApiException;
import com.rental.repository.*;
import com.rental.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final DrivingLicenseRepository licenseRepository;
    private final NotificationRepository notificationRepository;

    public BookingServiceImpl(BookingRepository bookingRepository, VehicleRepository vehicleRepository,
                              UserRepository userRepository, DrivingLicenseRepository licenseRepository,
                              NotificationRepository notificationRepository) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.licenseRepository = licenseRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Booking createBooking(Long userId, BookingRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null ||
                !request.getEndDate().isAfter(request.getStartDate())) {
            throw new ApiException("End date must be after start date", HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElseThrow(() -> new ApiException("Vehicle not found", HttpStatus.NOT_FOUND));
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) throw new ApiException("Vehicle is not available", HttpStatus.CONFLICT);
        if (!bookingRepository.findOverlappingBookings(vehicle.getId(), request.getStartDate(), request.getEndDate()).isEmpty()) {
            throw new ApiException("Vehicle already booked for the selected dates", HttpStatus.CONFLICT);
        }
        DrivingLicense license = licenseRepository.findById(request.getLicenseId())
                .orElseThrow(() -> new ApiException("Driving license not found. Please upload one first.", HttpStatus.BAD_REQUEST));
        if (!license.getUser().getId().equals(userId)) throw new ApiException("This license does not belong to you", HttpStatus.FORBIDDEN);
        if (!license.isVerified()) throw new ApiException("Driving license must be verified before booking", HttpStatus.BAD_REQUEST);

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        BigDecimal totalAmount = vehicle.getPricePerDay().multiply(BigDecimal.valueOf(days));
        return bookingRepository.save(Booking.builder().user(user).vehicle(vehicle).shop(vehicle.getShop()).drivingLicense(license)
                .startDate(request.getStartDate()).endDate(request.getEndDate()).totalAmount(totalAmount)
                .status(BookingStatus.PENDING_PAYMENT).build());
    }

    @Override public List<Booking> getUserBookings(Long userId) { return bookingRepository.findByUserId(userId); }
    @Override public List<Booking> getShopBookings(Long shopId) { return bookingRepository.findByShopId(shopId); }
    @Override public List<Booking> getOwnerShopBookings(Long ownerId, Long shopId) { List<Booking> b=bookingRepository.findByShopId(shopId); if(b.stream().anyMatch(x->!x.getShop().getOwner().getId().equals(ownerId))) throw new ApiException("You do not have access to this shop",HttpStatus.FORBIDDEN); return b; }
    @Override public Booking getByIdOrThrow(Long bookingId) { return bookingRepository.findById(bookingId).orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND)); }

    @Override public void markConfirmed(Long bookingId) {
        Booking booking = getByIdOrThrow(bookingId);
        booking.setStatus(BookingStatus.PENDING_OWNER_APPROVAL);
        bookingRepository.save(booking);
        notifyUser(booking, "Payment successful", "Your booking #" + booking.getId() + " is waiting for owner approval.");
    }

    @Override public Booking approveBooking(Long ownerId, Long bookingId) {
        Booking b=getByIdOrThrow(bookingId); assertOwner(ownerId,b);
        if (b.getStatus()!=BookingStatus.PENDING_OWNER_APPROVAL) throw new ApiException("Only paid pending bookings can be approved",HttpStatus.BAD_REQUEST);
        b.setStatus(BookingStatus.CONFIRMED);
        b=bookingRepository.save(b);
        Vehicle v=b.getVehicle(); v.setStatus(VehicleStatus.RENTED); vehicleRepository.save(v);
        notifyUser(b,"Booking confirmed","Owner approved booking #"+b.getId()+" for "+b.getVehicle().getName()+".");
        return b;
    }

    @Override public Booking rejectBooking(Long ownerId, Long bookingId) {
        Booking b=getByIdOrThrow(bookingId); assertOwner(ownerId,b);
        if (b.getStatus()!=BookingStatus.PENDING_OWNER_APPROVAL) throw new ApiException("Only paid pending bookings can be rejected",HttpStatus.BAD_REQUEST);
        b.setStatus(BookingStatus.REJECTED); b=bookingRepository.save(b);
        notifyUser(b,"Booking rejected","Owner rejected booking #"+b.getId()+". Contact support if you need help with the payment.");
        return b;
    }

    @Override public Booking markCompleted(Long ownerId, Long bookingId) {
        Booking b=getByIdOrThrow(bookingId); assertOwner(ownerId,b);
        if (b.getStatus()!=BookingStatus.CONFIRMED && b.getStatus()!=BookingStatus.ONGOING) throw new ApiException("Only CONFIRMED or ONGOING bookings can be marked as completed",HttpStatus.BAD_REQUEST);
        b.setStatus(BookingStatus.COMPLETED); b=bookingRepository.save(b);
        Vehicle v=b.getVehicle(); v.setStatus(VehicleStatus.AVAILABLE); vehicleRepository.save(v);
        notifyUser(b,"Rental completed","Booking #"+b.getId()+" has been marked completed. You can now leave a review.");
        return b;
    }

    @Override public Booking cancelBooking(Long userId, Long bookingId) {
        Booking b=getByIdOrThrow(bookingId);
        if(!b.getUser().getId().equals(userId)) throw new ApiException("Not allowed",HttpStatus.FORBIDDEN);
        if(b.getStatus()==BookingStatus.COMPLETED || b.getStatus()==BookingStatus.CANCELLED || b.getStatus()==BookingStatus.REJECTED) throw new ApiException("Booking cannot be cancelled",HttpStatus.BAD_REQUEST);
        b.setStatus(BookingStatus.CANCELLED); b=bookingRepository.save(b);
        if(b.getVehicle().getStatus()==VehicleStatus.RENTED) { b.getVehicle().setStatus(VehicleStatus.AVAILABLE); vehicleRepository.save(b.getVehicle()); }
        notifyUser(b,"Booking cancelled","Booking #"+b.getId()+" has been cancelled.");
        return b;
    }

    private void assertOwner(Long ownerId, Booking b){
        if(!b.getShop().getOwner().getId().equals(ownerId)) throw new ApiException("You do not have access to this booking",HttpStatus.FORBIDDEN);
    }
    private void notifyUser(Booking b,String title,String message){
        notificationRepository.save(Notification.builder().user(b.getUser()).title(title).message(message).build());
    }
}
