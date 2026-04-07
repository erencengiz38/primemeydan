package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.TournamentApplicationPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentApplicationPlayerRepository extends JpaRepository<TournamentApplicationPlayer, Long> {

    List<TournamentApplicationPlayer> findByTournamentApplicationId(Long tournamentApplicationId);

    List<TournamentApplicationPlayer> findByClanMemberId(Long clanMemberId);

    void deleteByTournamentApplicationId(Long tournamentApplicationId);
}
