package com.meydan.meydan.models.entities;

import com.meydan.meydan.models.enums.OrganizationRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "organization_memberships")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMembership {

    @EmbeddedId
    private OrganizationMembershipId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("organizationId")
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationRole role;

    @Column(name = "can_create_tournament", nullable = false)
    private Boolean canCreateTournament = true; // Varsayılan olarak turnuva oluşturma izni verilir
}