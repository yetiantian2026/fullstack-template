package com.yetiantian.campus_erp.dto;

import java.math.BigDecimal;

public record ProjectSummary(
        Long id,
        String name,
        BigDecimal total,
        BigDecimal frozen,
        BigDecimal used,
        BigDecimal available
) {
}