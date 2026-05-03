package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.Clan;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClanRepository extends JpaRepository<Clan, Long> {

    List<Clan> findByCategoryIdAndIsActiveTrue(Long categoryId);

    Page<Clan> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    List<Clan> findByIsActiveTrue();

    Page<Clan> findByIsActiveTrue(Pageable pageable);

    boolean existsByCategoryIdAndNameAndIsActiveTrue(Long categoryId, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Clan c WHERE c.id = :clanId")
    Optional<Clan> findByIdForUpdate(@Param("clanId") Long clanId);
}
