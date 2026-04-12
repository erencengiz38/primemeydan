package com.meydan.meydan.models.entities;

import com.meydan.meydan.models.enums.ParticipantType;
import com.meydan.meydan.models.enums.TournamentFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Table(name = "turnuva")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Turnuva extends BaseEntity {

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Date start_date;
    private Date finish_date;

    @Column(name = "registration_deadline")
    private Date registrationDeadline;

    private String imageUrl;
    private Double reward_amount;
    private String reward_currency;
    private String player_format; // Örn: "5v5", "Duo"

    @Enumerated(EnumType.STRING)
    private ParticipantType participantType; // SOLO, CLAN

    @Enumerated(EnumType.STRING)
    private TournamentFormat tournamentFormat; // SCRIM, STAGE_BASED

    @Column(name = "organization_id")
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private Boolean isActive = true;

    // --- YENİ EKLENEN ESNEK MOTOR ALANLARI ---
    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "min_participants")
    private Integer minParticipants;

    @Column(name = "team_size")
    private Integer teamSize; // PUBG için 4, CS için 5, Solo için 1

    @Column(name = "match_capacity")
    private Integer matchCapacity; // Bir maçta kaç takım/kişi yarışır? (CS: 2, PUBG: 20)

    @Column(name = "current_participants_count")
    private Integer currentParticipantsCount = 0;
}