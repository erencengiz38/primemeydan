package com.meydan.meydan.models.entities;

import com.meydan.meydan.models.enums.TargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "active_dopings", indexes = {
    @Index(name = "idx_doping_target", columnList = "target_type, target_id")
})
@Getter
@Setter
public class ActiveDoping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Satın alan kişi

    @Column(name = "target_id", nullable = false)
    private Long targetId; // Hangi klanın/ilanın ID'si olduğu

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType; // Hangi tabloya ait olduğu (CLAN, ORGANIZATION, TOURNAMENT, LISTING)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private DopingPackage dopingPackage;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Boolean isActive = true;
}
