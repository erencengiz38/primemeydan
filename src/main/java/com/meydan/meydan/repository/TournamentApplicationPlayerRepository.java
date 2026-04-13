package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.TournamentApplicationPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TournamentApplicationPlayerRepository extends JpaRepository<TournamentApplicationPlayer, Long> {

    List<TournamentApplicationPlayer> findByTournamentApplicationId(Long tournamentApplicationId);

    List<TournamentApplicationPlayer> findByClanMemberId(Long clanMemberId);

    void deleteByTournamentApplicationId(Long tournamentApplicationId);

    // Bir oyuncunun belirli tarihler arasında "APPROVED" statüsünde bir turnuvada olup olmadığını kontrol eder
    @Query("SELECT CASE WHEN COUNT(tap) > 0 THEN true ELSE false END " +
           "FROM TournamentApplicationPlayer tap " +
           "JOIN tap.tournamentApplication ta " +
           "JOIN ta.tournament t " +
           "WHERE tap.userId = :userId " +
           "AND ta.status = 'APPROVED' " +
           "AND t.isActive = true " +
           "AND t.start_date <= :endDate " +
           "AND t.finish_date >= :startDate")
    boolean isPlayerBusyInDateRange(@Param("userId") Long userId, 
                                    @Param("startDate") Date startDate, 
                                    @Param("endDate") Date endDate);
}
