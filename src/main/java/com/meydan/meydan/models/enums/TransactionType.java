package com.meydan.meydan.models.enums;

public enum TransactionType {
    DEPOSIT,       // Gerçek para (TL) yükleme
    WITHDRAWAL,    // Gerçek para çekme
    PURCHASE,      // Gerçek para ile mağazadan bir şey alma
    REFUND,        // İade
    COIN_REWARD,   // Turnuva/Görev ödülü olarak Meydan Coin kazanma
    COIN_SPEND     // Meydan Coin ile mağazadan bir şey alma
}
