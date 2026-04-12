package com.meydan.meydan.models.entities;

import com.meydan.meydan.models.enums.CurrencyType;
import com.meydan.meydan.models.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_type", nullable = false)
    private CurrencyType currencyType; // TRY veya MEYDAN_COIN

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();
}
