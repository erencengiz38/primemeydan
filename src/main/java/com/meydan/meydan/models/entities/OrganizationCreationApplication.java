package com.meydan.meydan.models.entities;

import com.meydan.meydan.models.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_creation_applications") // TABLO ADI DEĞİŞTİRİLDİ (ÇAKIŞMAYI ÖNLEMEK İÇİN)
@Getter
@Setter
public class OrganizationCreationApplication extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id")
    private Long categoryId;

    private String organizationName;

    @Column(length = 1000)
    private String description;
    
    @Column(name = "logo_url")
    private String logoUrl;

    private Boolean hasPreviousExperience;

    @Column(length = 1000)
    private String previousExperienceDetails;

    @Column(length = 1000)
    private String managementPlan;

    private String plannedGames;

    @Column(length = 1000)
    private String reachPlan;

    private String discordLink;

    private String socialMediaLinks;

    private Boolean hasPrizes;

    private String averagePrizeAmount;

    @Column(length = 1000)
    private String reasonForApplying;

    private Boolean rulesAccepted;

    private Boolean noVictimAccepted;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(length = 1000)
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();
}