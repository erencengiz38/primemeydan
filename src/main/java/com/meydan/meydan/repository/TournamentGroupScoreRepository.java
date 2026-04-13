package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.TournamentGroupScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentGroupScoreRepository extends JpaRepository<TournamentGroupScore, Long> {
    List<TournamentGroupScore> findByGroupIdOrderByScoreDesc(Long groupId);
    Optional<TournamentGroupScore> findByGroupIdAndApplicationId(Long groupId, Long applicationId);
}
