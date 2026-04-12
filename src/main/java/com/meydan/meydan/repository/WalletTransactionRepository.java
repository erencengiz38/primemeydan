package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletIdOrderByTransactionDateDesc(Long walletId);
}
