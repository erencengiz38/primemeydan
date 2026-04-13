package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_matches")
@Getter
@Setter
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Turnuva tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private TournamentStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_1_id") // Nullable if waiting for winner of previous match
    private TournamentApplication team1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_2_id")
    private TournamentApplication team2;

    private Integer team1Score;
    private Integer team2Score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private TournamentApplication winner;

    @Column(nullable = false)
    private Integer roundNumber = 1;

    private LocalDateTime matchDate;

    // Sonucu kim, ne zaman girdi
    private Long reportedById;
    private LocalDateTime reportedAt;
}
