package com.meydan.meydan.request.Auth.Category;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCategoryRequestBody {
    @NotBlank(message = "Kategori adı boş olamaz")
    @Size(max = 100, message = "Kategori adı 100 karakterden uzun olamaz")
    private String name;

    @NotBlank(message = "Resim URL'si boş olamaz")
    private String image;

    @NotBlank(message = "Açıklama boş olamaz")
    @Size(max = 500, message = "Açıklama 500 karakterden uzun olamaz")
    private String description;

    private Long parentId; // Ana kategori ID'si (nullable, sub-category için)
}
