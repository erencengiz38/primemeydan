package com.meydan.meydan.dto;

import com.meydan.meydan.models.enums.CategoryType;
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
    private CategoryType type;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
