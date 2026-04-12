package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "tournament_group")
@Getter
@Setter
public class TournamentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private TournamentStage stage;

    @Column(nullable = false)
    private String name; // A Grubu, B Grubu veya 1. Eşleşme, 2. Eşleşme vb.
    
    @Column(nullable = false)
    private Integer maxParticipants; // Grupta yer alabilecek maksimum katılımcı sayısı
}
