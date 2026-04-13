package com.meydan.meydan.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletResponseDTO {
    private Long id;
    private Long userId;
    private BigDecimal realBalance;
    private BigDecimal meydanCoin;
    private LocalDateTime lastUpdatedAt;
}
