package com.meydan.meydan.request.Auth.Turnuva;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplyToTournamentRequestBody {

    @NotNull(message = "Turnuva ID boş olamaz")
    private Long tournamentId;

    private Long clanId; // Nullable - for representing a clan in 1v1 or for Team apps

    private List<Long> selectedClanMemberIds; // For team-based tournaments, selected players
}
