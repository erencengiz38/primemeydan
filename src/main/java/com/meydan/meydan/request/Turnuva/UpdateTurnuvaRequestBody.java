package com.meydan.meydan.request.Turnuva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTurnuvaRequestBody {
    @NotNull(message = "Turnuva ID boş olamaz")
    private Long id;

    @NotNull(message = "Kategori ID boş bırakılamaz")
    private Long categoryId;

    @NotBlank(message = "Başlık boş bırakılamaz")
    private String title;

    @NotBlank(message = "Açıklama boş bırakılamaz")
    private String description;
    @NotNull(message = "Organizasyon ID boş olamaz")
    private Long organizationId;
    @NotNull(message = "Başlangıç tarihi gereklidir")
    private Date start_date;

    @NotNull(message = "Bitiş tarihi gereklidir")
    private Date finish_date;

    private Boolean isActive;

    private String link;

    private String imageUrl;

    private Double reward_amount;
    private String reward_currency;
    private String player_format;
}
