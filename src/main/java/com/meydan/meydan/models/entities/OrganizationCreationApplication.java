package com.meydan.meydan.models.entities;

import com.meydan.meydan.models.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "organization_applications")
@Getter
@Setter
public class OrganizationCreationApplication extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String organizationName;

    @Column(length = 1000)
    private String description;

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
}