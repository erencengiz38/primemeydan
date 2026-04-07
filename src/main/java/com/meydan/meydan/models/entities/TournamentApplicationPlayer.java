package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_application_player")
@Data
public class TournamentApplicationPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_application_id", nullable = false)
    private TournamentApplication tournamentApplication;

    @Column(name = "clan_member_id", nullable = false)
    private Long clanMemberId; // Reference to ClanMember.id

    @Column(name = "user_id", nullable = false)
    private Long userId; // For quick access without joining

    @Column(nullable = false)
    private LocalDateTime selectedAt = LocalDateTime.now();
}
