package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.TournamentGroupAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentGroupAssignmentRepository extends JpaRepository<TournamentGroupAssignment, Long> {
    List<TournamentGroupAssignment> findByGroupId(Long groupId);
    void deleteByGroupId(Long groupId); // Ayrıştırmayı sıfırlamak için
}
