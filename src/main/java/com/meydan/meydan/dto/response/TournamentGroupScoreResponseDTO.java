package com.meydan.meydan.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TournamentGroupScoreResponseDTO {
    private Long id;
    private Long groupId;
    private Long applicationId;
    private String clanName;
    private Integer score;
    private Integer placement;
    private Boolean isAdvanced;
    private LocalDateTime updatedAt;
    private Long reportedById;
}
