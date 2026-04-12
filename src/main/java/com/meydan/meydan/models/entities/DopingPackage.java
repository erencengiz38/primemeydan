package com.meydan.meydan.models.entities;

import com.meydan.meydan.models.enums.DopingEffectType;
import com.meydan.meydan.models.enums.TargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "doping_packages")
@Getter
@Setter
public class DopingPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description; // Örn: 'İlanınızı 24 saat altın çerçeveyle gösterir'

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer durationHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DopingEffectType effectType;
    
    @Column(nullable = false)
    private Boolean isActive = true;
}
