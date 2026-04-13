package com.meydan.meydan.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletTransactionResponseDTO {
    private Long id;
    private String type; // DEPOSIT, WITHDRAWAL, PURCHASE, vb.
    private String currencyType; // TRY, MEYDAN_COIN
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
}
