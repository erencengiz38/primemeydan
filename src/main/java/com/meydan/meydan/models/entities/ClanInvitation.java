package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clan_invitation")
@Getter
@Setter
public class ClanInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clan_id", nullable = false)
    private Clan clan;

    @Column(name = "user_id", nullable = false)
    private Long userId; // The user being invited or applying

    @Column(name = "inviter_id", nullable = false)
    private Long inviterId; // The user who initiated the action (clan owner/manager or the applicant)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClanInvitationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClanInvitationStatus status = ClanInvitationStatus.PENDING;

    @Column(length = 255)
    private String reason; // Reason for rejection

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime respondedAt;
}
