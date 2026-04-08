package com.meydan.meydan.request.Auth.Organization;

import lombok.Data;

@Data
public class CreateOrganizationRequestBody {
    private String name;
    private String description;
    private String logoUrl;
    private Long categoryId;
}