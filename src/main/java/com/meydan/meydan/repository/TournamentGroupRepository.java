package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.TournamentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentGroupRepository extends JpaRepository<TournamentGroup, Long> {
    List<TournamentGroup> findByStageId(Long stageId);
}
