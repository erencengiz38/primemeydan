package com.meydan.meydan.request.Clan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddClanRequestBody {

    @NotBlank(message = "Clan adı boş olamaz")
    @Size(max = 100, message = "Clan adı 100 karakterden uzun olamaz")
    private String name;

    @NotNull(message = "Kategori ID boş olamaz")
    private Long categoryId;

    @Size(max = 1000, message = "Açıklama 1000 karakterden uzun olamaz")
    private String description;

    private String logo;
}
