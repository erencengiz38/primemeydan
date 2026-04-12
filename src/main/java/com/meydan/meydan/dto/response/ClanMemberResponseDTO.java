package com.meydan.meydan.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClanMemberResponseDTO {
    private Long id;
    private Long userId;
    private Long clanId;
    private String clanName; // Yardımcı bilgi
    private Long categoryId;
    private String role;
    private LocalDateTime joinedAt;
    private Boolean isActive;
}
