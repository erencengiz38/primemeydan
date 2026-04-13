package com.meydan.meydan.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrganizationQuotaResponseDTO {
    private Long organizationId;
    private BigDecimal weeklyLimit;
    private BigDecimal currentSpent;
    private BigDecimal remainingQuota;
    private LocalDateTime lastResetDate;
}
