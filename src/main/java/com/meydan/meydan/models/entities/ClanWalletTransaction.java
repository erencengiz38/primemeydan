package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clan_wallet_transactions")
@Getter
@Setter
public class ClanWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clan_id", nullable = false)
    private Clan clan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // İşlemi yapan kullanıcı (turnuva başvurusu veya bağış yapan kişi)

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String transactionType; // DEPOSIT (Bağış) veya WITHDRAWAL (Turnuva Girişi) vb.

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    public ClanWalletTransaction() {
    }

    public ClanWalletTransaction(Clan clan, User user, Double amount, String transactionType, String description) {
        this.clan = clan;
        this.user = user;
        this.amount = amount;
        this.transactionType = transactionType;
        this.description = description;
        this.transactionDate = LocalDateTime.now();
    }
}
