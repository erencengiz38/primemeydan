package com.meydan.meydan.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TournamentMatchResponseDTO {
    private Long id;
    private Long tournamentId;
    private Long stageId;
    
    private Long team1ApplicationId;
    private String team1ClanName;
    private Integer team1Score;
    
    private Long team2ApplicationId;
    private String team2ClanName;
    private Integer team2Score;
    
    private Long winnerApplicationId;
    
    private Integer roundNumber;
    private LocalDateTime matchDate;
    private LocalDateTime reportedAt;
}
