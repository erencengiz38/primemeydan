package com.meydan.meydan.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TournamentApplicationPlayerResponseDTO {
    private Long id;
    private Long clanMemberId;
    private Long userId;
    private LocalDateTime selectedAt;
}
