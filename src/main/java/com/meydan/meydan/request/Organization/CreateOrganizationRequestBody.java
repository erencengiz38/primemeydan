package com.meydan.meydan.request.Organization;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrganizationRequestBody {
    @NotNull(message = "Kategori ID boş olamaz")
    @Min(value = 1, message = "Geçerli bir kategori seçmelisiniz (ID 1 veya daha büyük olmalı)")
    private Long categoryId;
    private String name;
    private String description;
    private String logoUrl;
}