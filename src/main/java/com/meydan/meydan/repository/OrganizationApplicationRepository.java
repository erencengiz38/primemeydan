package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.OrganizationApplication;
import com.meydan.meydan.models.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationApplicationRepository extends JpaRepository<OrganizationApplication, Long> {

    List<OrganizationApplication> findByOrganizationIdAndStatus(Long organizationId, ApplicationStatus status);

    boolean existsByOrganizationIdAndUserIdAndStatus(Long organizationId, Long userId, ApplicationStatus status);
}