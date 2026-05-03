package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_profile_settings")
@Getter
@Setter
public class UserProfileSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    // Görünürlük Ayarları
    @Column(nullable = false)
    private Boolean showProfile = true; // Profili görünür yap / Gizli yap

    @Column(nullable = false)
    private Boolean showClans = true; // Klanlık bilgisi göster

    @Column(nullable = false)
    private Boolean showRatings = true; // Kritikleri göster

    @Column(nullable = false)
    private Boolean showBio = true; // Bio/Açıklama göster

    @Column(nullable = false)
    private Boolean allowDirectMessages = true; // Mesaj gönderime izin ver

    // Bio
    @Column(length = 500)
    private String bio;

    // Profil Kapanışı
    @Column(nullable = false)
    private Boolean isPrivate = false; // Tamamen özel / Sadece arkadaşlara göster

    public UserProfileSettings() {
    }

    public UserProfileSettings(Long userId) {
        this.userId = userId;
        this.showProfile = true;
        this.showClans = true;
        this.showRatings = true;
        this.showBio = true;
        this.allowDirectMessages = true;
        this.isPrivate = false;
    }
}

