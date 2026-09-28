package com.rental.repository;

import com.rental.entity.Booking;
import com.rental.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);
    List<Booking> findByShopId(Long shopId);
    List<Booking> findByShopIdAndStatus(Long shopId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId " +
           "AND b.status IN ('PENDING_OWNER_APPROVAL','CONFIRMED','ONGOING') " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<Booking> findOverlappingBookings(@Param("vehicleId") Long vehicleId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT FUNCTION('MONTH', b.createdAt), FUNCTION('YEAR', b.createdAt), " +
           "COUNT(b), COALESCE(SUM(b.totalAmount), 0) " +
           "FROM Booking b WHERE b.shop.id = :shopId AND b.status = 'COMPLETED' " +
           "AND FUNCTION('YEAR', b.createdAt) = :year " +
           "GROUP BY FUNCTION('MONTH', b.createdAt), FUNCTION('YEAR', b.createdAt) " +
           "ORDER BY FUNCTION('MONTH', b.createdAt)")
    List<Object[]> getMonthlyStats(@Param("shopId") Long shopId, @Param("year") int year);

    @Query("SELECT FUNCTION('YEAR', b.createdAt), COUNT(b), COALESCE(SUM(b.totalAmount), 0) " +
           "FROM Booking b WHERE b.shop.id = :shopId AND b.status = 'COMPLETED' " +
           "GROUP BY FUNCTION('YEAR', b.createdAt) ORDER BY FUNCTION('YEAR', b.createdAt)")
    List<Object[]> getYearlyStats(@Param("shopId") Long shopId);
}
