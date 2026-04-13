package com.meydan.meydan.dto.response;

import com.meydan.meydan.models.enums.OrganizationRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberResponseDTO {
    private Long userId;
    private String userName;
    private String userTag;
    private OrganizationRole role;
    private Long organizationId;
    private String organizationName;
}
