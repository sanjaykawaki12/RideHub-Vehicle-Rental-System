package com.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStat {
    private String label;   // e.g. "Jan-2026" or "2026"
    private long totalBookings;
    private BigDecimal totalRevenue;
}
