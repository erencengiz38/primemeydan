package com.meydan.meydan.request.Auth.Turnuva;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddTurnuvaRequestBody {
    @NotNull(message = "Kategori ID boş bırakılamaz")
    private Long categoryId;
    
    @NotBlank(message = "Başlık boş bırakılamaz")
    private String title;
    
    @NotBlank(message = "Açıklama boş bırakılamaz")
    private String description;
    
    @NotNull(message = "Başlangıç tarihi gereklidir")
    private Date start_date;
    
    @NotNull(message = "Bitiş tarihi gereklidir")
    private Date finish_date;
    
    private Boolean isActive;
    
    // Sosyal ağ linki - Instagram, WhatsApp, Discord, Telegram
    // link_type otomatik belirlenecek, kullanıcıdan alınmayacak
    private String link;

    private String imageUrl;

    private Double reward_amount;
    private String reward_currency;
    private String player_format;
}
