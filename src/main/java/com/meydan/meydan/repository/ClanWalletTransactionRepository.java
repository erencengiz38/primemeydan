package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.ClanWalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClanWalletTransactionRepository extends JpaRepository<ClanWalletTransaction, Long> {
    List<ClanWalletTransaction> findByClanIdOrderByTransactionDateDesc(Long clanId);
}
