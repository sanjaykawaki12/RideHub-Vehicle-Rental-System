package com.rental.service;

import com.rental.dto.DashboardSummary;

public interface DashboardService {
    DashboardSummary getMonthlyDashboard(Long ownerId, Long shopId, int year);
    DashboardSummary getYearlyDashboard(Long ownerId, Long shopId);
}
