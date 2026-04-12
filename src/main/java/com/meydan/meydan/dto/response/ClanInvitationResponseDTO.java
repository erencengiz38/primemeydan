package com.meydan.meydan.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClanInvitationResponseDTO {
    private Long id;
    private Long clanId;
    private String clanName; // Yardımcı bilgi
    private Long userId; // Hedef kullanıcı
    private String userName; // Eklenen alan: Kullanıcı adı
    private String userTag;  // Eklenen alan: Kullanıcı tag'i
    private Long inviterId; // İşlemi başlatan
    private String type; // INVITATION or APPLICATION
    private String status; // PENDING, ACCEPTED vs.
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
