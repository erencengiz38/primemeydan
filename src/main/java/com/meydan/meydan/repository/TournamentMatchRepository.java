package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {
    List<TournamentMatch> findByTournamentIdOrderByRoundNumberAscMatchDateAsc(Long tournamentId);
}
