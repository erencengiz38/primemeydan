package com.meydan.meydan.dto.response;

import lombok.Data;
import java.util.Date;
import java.util.UUID;

@Data
public class TurnuvaResponseDTO {
    private Long id;
    private UUID oid;
    private Long organizationId;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private Date start_date;
    private Date finish_date;
    private Boolean isActive;
    private String imageUrl;
    private String link;
    private String link_type;
    private Double reward_amount;
    private String reward_currency;
    private String player_format;
    private String participantType;
    private String tournamentFormat;
    private Date registrationDeadline;
    private Integer maxParticipants;
    private Integer minParticipants;
    private Integer teamSize;
    private Integer matchCapacity;
    private Integer currentParticipantsCount;
}
