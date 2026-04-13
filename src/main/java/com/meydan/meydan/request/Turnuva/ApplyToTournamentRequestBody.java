package com.meydan.meydan.request.Turnuva;

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

    @NotNull(message = "Klan ID boş olamaz")
    private Long clanId; 

    @NotNull(message = "Seçilen oyuncular (User ID'leri) boş olamaz")
    private List<Long> selectedClanMemberIds;
}
