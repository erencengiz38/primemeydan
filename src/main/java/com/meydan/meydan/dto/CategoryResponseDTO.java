package com.meydan.meydan.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private String image;
    private String description;
    private String slug;
    private Long parentId;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
