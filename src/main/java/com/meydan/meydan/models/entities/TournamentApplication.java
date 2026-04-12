package com.meydan.meydan.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meydan.meydan.models.enums.TournamentApplicationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tournament_application")
@Data
public class TournamentApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Turnuva tournament;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Applicant/Solo Player

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clan_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Sonsuz döngüyü ve gereksiz detayı engellemek için
    private Clan clan; // Nullable - for representing a clan in 1v1 or for Team apps

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentApplicationStatus status = TournamentApplicationStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    @Column(nullable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();

    @Column
    private LocalDateTime reviewedAt;

    // --- YENİ EKLENEN YOKLAMA (CHECK-IN) ALANI ---
    @Column(name = "is_checked_in", nullable = false)
    private Boolean isCheckedIn = false; // Başlangıçta herkes false, check-in yapınca true olacak

    @OneToMany(mappedBy = "tournamentApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"tournamentApplication", "hibernateLazyInitializer", "handler"})
    private List<TournamentApplicationPlayer> selectedPlayers;
}
