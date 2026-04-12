package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Getter
@Setter
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "real_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal realBalance = BigDecimal.ZERO; // Gerçek TL bakiyesi

    @Column(name = "meydan_coin", nullable = false, precision = 19, scale = 4)
    private BigDecimal meydanCoin = BigDecimal.ZERO; // Platform parası bakiyesi

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
