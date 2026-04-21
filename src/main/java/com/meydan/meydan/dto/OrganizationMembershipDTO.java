package com.meydan.meydan.dto;

import com.meydan.meydan.models.enums.OrganizationRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMembershipDTO {
    private Long organizationId;
    private String organizationName;
    private OrganizationRole role;
}
