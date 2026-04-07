package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.Clan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClanRepository extends JpaRepository<Clan, Long> {

    List<Clan> findByCategoryIdAndIsActiveTrue(Long categoryId);

    Page<Clan> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    List<Clan> findByIsActiveTrue();

    Page<Clan> findByIsActiveTrue(Pageable pageable);

    boolean existsByCategoryIdAndNameAndIsActiveTrue(Long categoryId, String name);
}
