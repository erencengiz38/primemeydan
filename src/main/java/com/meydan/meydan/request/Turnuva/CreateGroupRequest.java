package com.meydan.meydan.request.Turnuva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CreateGroupRequest {
    @NotBlank(message = "Grup adı boş olamaz")
    private String name;

    @NotNull(message = "Maksimum katılımcı sayısı boş olamaz")
    @Min(value = 2, message = "En az 2 katılımcı olmalıdır")
    private Integer maxParticipants;
}