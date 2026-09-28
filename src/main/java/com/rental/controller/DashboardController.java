package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.DashboardSummary;
import com.rental.security.AuthPrincipal;
import com.rental.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

@RestController
@RequestMapping("/api/owner/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // How many of each vehicle are available/rented right now + monthly revenue trend
    @GetMapping("/shops/{shopId}/monthly")
    public ResponseEntity<ApiResponse<DashboardSummary>> monthly(@AuthenticationPrincipal AuthPrincipal principal,
                                                                   @PathVariable Long shopId,
                                                                   @RequestParam(required = false) Integer year) {
        int y = year != null ? year : Year.now().getValue();
        DashboardSummary summary = dashboardService.getMonthlyDashboard(principal.id(), shopId, y);
        return ResponseEntity.ok(ApiResponse.ok("Monthly dashboard fetched", summary));
    }

    @GetMapping("/shops/{shopId}/yearly")
    public ResponseEntity<ApiResponse<DashboardSummary>> yearly(@AuthenticationPrincipal AuthPrincipal principal,
                                                                  @PathVariable Long shopId) {
        DashboardSummary summary = dashboardService.getYearlyDashboard(principal.id(), shopId);
        return ResponseEntity.ok(ApiResponse.ok("Yearly dashboard fetched", summary));
    }
}
