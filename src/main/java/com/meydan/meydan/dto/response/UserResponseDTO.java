package com.meydan.meydan.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class UserResponseDTO {
    private Long id;
    private UUID oid;
    private String display_name;
    private String tag;
}