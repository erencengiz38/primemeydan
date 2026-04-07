package com.meydan.meydan.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clan_member")
@Getter
@Setter
public class ClanMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clan_id", nullable = false)
    @JsonIgnoreProperties({"members", "hibernateLazyInitializer", "handler"})
    private Clan clan;

    @Column(name = "category_id", nullable = false)
    private Long categoryId; // For unique constraint and quick queries

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClanMemberRole role = ClanMemberRole.MEMBER;

    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean isActive = true;
}
