package com.meydan.meydan.request.Turnuva;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RewardReportItem {
    @NotNull(message = "Application ID boş olamaz")
    private Long applicationId;
    
    @NotNull(message = "Ödül miktarı boş olamaz")
    private BigDecimal amount;
}
