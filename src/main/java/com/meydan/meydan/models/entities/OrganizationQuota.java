package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization_quotas")
@Getter
@Setter
public class OrganizationQuota {

    @Id
    @Column(name = "org_id")
    private Long organizationId;

    @Column(name = "weekly_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal weeklyLimit = BigDecimal.ZERO;

    @Column(name = "current_spent", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentSpent = BigDecimal.ZERO;

    @Column(name = "last_reset_date", nullable = false)
    private LocalDateTime lastResetDate = LocalDateTime.now();
}
