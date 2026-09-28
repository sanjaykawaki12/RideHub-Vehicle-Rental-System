package com.rental.service.impl;

import com.rental.dto.DashboardStat;
import com.rental.dto.DashboardSummary;
import com.rental.entity.Shop;
import com.rental.entity.enums.VehicleStatus;
import com.rental.repository.BookingRepository;
import com.rental.repository.VehicleRepository;
import com.rental.service.DashboardService;
import com.rental.service.ShopService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ShopService shopService;

    public DashboardServiceImpl(BookingRepository bookingRepository, VehicleRepository vehicleRepository,
                                 ShopService shopService) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.shopService = shopService;
    }

    @Override
    public DashboardSummary getMonthlyDashboard(Long ownerId, Long shopId, int year) {
        Shop shop = shopService.getShopOrThrow(shopId);
        shopService.assertOwnership(ownerId, shop);

        List<Object[]> raw = bookingRepository.getMonthlyStats(shopId, year);
        List<DashboardStat> stats = raw.stream().map(row -> {
            int month = ((Number) row[0]).intValue();
            long count = ((Number) row[2]).longValue();
            BigDecimal revenue = (BigDecimal) row[3];
            String label = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + "-" + year;
            return DashboardStat.builder().label(label).totalBookings(count).totalRevenue(revenue).build();
        }).collect(Collectors.toList());

        return buildSummary(shopId, stats);
    }

    @Override
    public DashboardSummary getYearlyDashboard(Long ownerId, Long shopId) {
        Shop shop = shopService.getShopOrThrow(shopId);
        shopService.assertOwnership(ownerId, shop);

        List<Object[]> raw = bookingRepository.getYearlyStats(shopId);
        List<DashboardStat> stats = raw.stream().map(row -> {
            int year = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            BigDecimal revenue = (BigDecimal) row[2];
            return DashboardStat.builder().label(String.valueOf(year)).totalBookings(count).totalRevenue(revenue).build();
        }).collect(Collectors.toList());

        return buildSummary(shopId, stats);
    }

    private DashboardSummary buildSummary(Long shopId, List<DashboardStat> stats) {
        long total = vehicleRepository.countByShopId(shopId);
        long available = vehicleRepository.countByShopIdAndStatus(shopId, VehicleStatus.AVAILABLE);
        long rented = vehicleRepository.countByShopIdAndStatus(shopId, VehicleStatus.RENTED);
        long maintenance = vehicleRepository.countByShopIdAndStatus(shopId, VehicleStatus.MAINTENANCE);

        return DashboardSummary.builder()
                .totalVehicles(total)
                .availableVehicles(available)
                .rentedVehicles(rented)
                .maintenanceVehicles(maintenance)
                .stats(stats)
                .build();
    }
}
