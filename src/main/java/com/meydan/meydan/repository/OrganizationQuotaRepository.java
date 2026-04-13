package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.OrganizationQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OrganizationQuotaRepository extends JpaRepository<OrganizationQuota, Long> {

    @Modifying
    @Query("UPDATE OrganizationQuota q SET q.currentSpent = 0, q.lastResetDate = :now")
    int resetAllQuotas(@Param("now") LocalDateTime now);
}
