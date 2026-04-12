package com.meydan.meydan.request.Turnuva;

import com.meydan.meydan.models.enums.TournamentApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RespondToTournamentInviteRequest {
    @NotNull(message = "Yanıt durumu boş olamaz")
    private TournamentApplicationStatus status; // APPROVED, REJECTED, veya SUBSTITUTE
    
    private String reason; // Red veya yedek olma sebebi

    private Long clanId; // Klan turnuvasıysa (ParticipantType.CLAN) kabul ederken zorunlu
    
    private List<Long> selectedClanMemberIds; // Seçilen oyuncular (User ID'leri), opsiyonel
}
