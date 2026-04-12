package com.meydan.meydan.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TournamentApplicationResponseDTO {
    private Long id;
    private Long tournamentId;
    private String tournamentTitle; // Sadece başlık
    private Long userId; // Başvuran kişi
    private Long clanId; // Nullable
    private String clanName; // Eğer clan başvurusuysa
    private String status;
    private String rejectionReason;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private Boolean isCheckedIn;
    
    // Alt liste olarak sadece basit DTO
    private List<TournamentApplicationPlayerResponseDTO> selectedPlayers;
}
