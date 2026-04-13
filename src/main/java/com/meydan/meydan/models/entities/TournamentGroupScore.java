package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_group_scores")
@Getter
@Setter
public class TournamentGroupScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private TournamentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private TournamentApplication application;

    @Column(nullable = false)
    private Integer score = 0; // Takımın o gruptaki puanı (Örn: Kill puanı + Sıralama puanı)

    private Integer placement; // Gruptaki sıralaması (1., 2. vb.)

    @Column(nullable = false)
    private Boolean isAdvanced = false; // Bir sonraki aşamaya geçmeye hak kazandı mı?

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    private Long reportedById; // Skoru giren hakem/admin
}
