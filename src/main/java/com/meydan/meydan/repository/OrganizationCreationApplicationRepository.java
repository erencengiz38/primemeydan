package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.OrganizationCreationApplication;
import com.meydan.meydan.models.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationCreationApplicationRepository extends JpaRepository<OrganizationCreationApplication, Long> {
    List<OrganizationCreationApplication> findByStatus(ApplicationStatus status);
    List<OrganizationCreationApplication> findByUserId(Long userId);
    boolean existsByUserIdAndStatus(Long userId, ApplicationStatus status);
}