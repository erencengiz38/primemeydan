package com.meydan.meydan.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ClanResponseDTO {
    private Long id;
    private UUID oid;
    private String name;
    private String categoryName;
    private String description;
    private String logo;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
