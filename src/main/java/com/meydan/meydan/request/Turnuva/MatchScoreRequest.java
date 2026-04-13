package com.meydan.meydan.request.Turnuva;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchScoreRequest {
    @NotNull(message = "Takım 1'in skoru boş olamaz")
    private Integer team1Score;

    @NotNull(message = "Takım 2'nin skoru boş olamaz")
    private Integer team2Score;

    @NotNull(message = "Kazanan takımın ID'si boş olamaz (TournamentApplication ID)")
    private Long winnerApplicationId;
}
