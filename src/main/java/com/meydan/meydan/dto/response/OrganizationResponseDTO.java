package com.meydan.meydan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponseDTO {
    private Long id;
    private UUID oid;
    private Long categoryId;
    private String name;
    private String description;
    private String logoUrl;
    private LocalDateTime createdAt;
}
