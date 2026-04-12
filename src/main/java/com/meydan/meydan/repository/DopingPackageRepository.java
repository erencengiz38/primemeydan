package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.DopingPackage;
import com.meydan.meydan.models.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DopingPackageRepository extends JpaRepository<DopingPackage, Long> {
    List<DopingPackage> findByIsActiveTrue();
    List<DopingPackage> findByTargetTypeAndIsActiveTrue(TargetType targetType);
}
