package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.TournamentApplication;
import com.meydan.meydan.models.entities.TournamentApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentApplicationRepository extends JpaRepository<TournamentApplication, Long> {

    List<TournamentApplication> findByTournamentId(Long tournamentId);

    Page<TournamentApplication> findByTournamentId(Long tournamentId, Pageable pageable);

    List<TournamentApplication> findByUserId(Long userId);

    List<TournamentApplication> findByClanId(Long clanId);

    List<TournamentApplication> findByTournamentIdAndStatus(Long tournamentId, TournamentApplicationStatus status);

    Optional<TournamentApplication> findByTournamentIdAndUserId(Long tournamentId, Long userId);

    Optional<TournamentApplication> findByTournamentIdAndClanId(Long tournamentId, Long clanId);

    boolean existsByTournamentIdAndUserId(Long tournamentId, Long userId);

    boolean existsByTournamentIdAndClanId(Long tournamentId, Long clanId);
}
