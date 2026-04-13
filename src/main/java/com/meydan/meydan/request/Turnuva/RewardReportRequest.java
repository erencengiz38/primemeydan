package com.meydan.meydan.request.Turnuva;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RewardReportRequest {
    @NotEmpty(message = "Ödül raporu listesi boş olamaz")
    private List<RewardReportItem> rewards;
}
