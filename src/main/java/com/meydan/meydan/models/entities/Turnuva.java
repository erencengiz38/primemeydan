package com.meydan.meydan.models.entities;

import com.meydan.meydan.models.enums.ApplicationStatus;
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
    private TournamentFormat tournamentFormat; // SCRIM, STAGE_BASED

    @Column(name = "organization_id")
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private Boolean isActive = true;

    // --- YENİ EKLENEN ONAY ALANLARI ---
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApplicationStatus approvalStatus = ApplicationStatus.PENDING;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    // --- YENİ EKLENEN ESNEK MOTOR ALANLARI ---
    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "min_participants")
    private Integer minParticipants;

    // Yeni: Kadro büyüklükleri
    @Column(name = "min_team_size")
    private Integer minTeamSize;

    @Column(name = "max_team_size")
    private Integer maxTeamSize;

    @Column(name = "match_capacity")
    private Integer matchCapacity; // Bir maçta kaç takım/kişi yarışır? (CS: 2, PUBG: 20)

    @Column(name = "current_participants_count")
    private Integer currentParticipantsCount = 0;
}