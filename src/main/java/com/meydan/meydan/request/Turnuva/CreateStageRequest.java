package com.meydan.meydan.request.Turnuva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateStageRequest {
    @NotBlank(message = "Aşama adı boş olamaz")
    private String name;

    @NotNull(message = "Sıralama sırası boş olamaz")
    private Integer sequenceOrder;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
