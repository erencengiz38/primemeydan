package com.meydan.meydan.request;

import lombok.Data;

@Data
public class OrganizationApplyRequest {
    private String organizationName;
    private String description;
    private Boolean hasPreviousExperience;
    private String previousExperienceDetails;
    private String managementPlan;
    private String plannedGames;
    private String reachPlan;
    private String discordLink;
    private String socialMediaLinks;
    private Boolean hasPrizes;
    private String averagePrizeAmount;
    private String reasonForApplying;
    private Boolean rulesAccepted;
    private Boolean noVictimAccepted;
}