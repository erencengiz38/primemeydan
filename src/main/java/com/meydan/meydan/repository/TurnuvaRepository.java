package com.meydan.meydan.repository;


import com.meydan.meydan.models.entities.Turnuva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TurnuvaRepository extends JpaRepository<Turnuva, Long> {
    List<Turnuva> findByOrganizationId(Long organizationId);

    Page<Turnuva> findByOrganizationId(Long organizationId, Pageable pageable);
}
