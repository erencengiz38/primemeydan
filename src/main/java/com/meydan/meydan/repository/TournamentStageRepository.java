package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.TournamentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentStageRepository extends JpaRepository<TournamentStage, Long> {
    List<TournamentStage> findByTurnuvaIdOrderBySequenceOrderAsc(Long turnuvaId);
}
