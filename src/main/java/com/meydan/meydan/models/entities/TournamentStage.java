package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tournament_stage")
@Getter
@Setter
public class TournamentStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turnuva_id", nullable = false)
    private Turnuva turnuva;

    @Column(nullable = false)
    private String name; // Örn: Ön Eleme, Grup Aşaması, Play-off

    @Column(nullable = false)
    private Integer sequenceOrder; // Aşamanın sırası (1, 2, 3...)

    @Column(nullable = true) // Tarihlerin null olabilmesi için
    private LocalDateTime startDate;

    @Column(nullable = true) // Tarihlerin null olabilmesi için
    private LocalDateTime endDate;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TournamentGroup> groups;
}
