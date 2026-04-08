package com.meydan.meydan.models.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMembershipId implements Serializable {
    private Long organizationId;
    private Long userId;
}