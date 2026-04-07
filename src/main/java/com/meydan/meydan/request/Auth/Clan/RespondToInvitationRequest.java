package com.meydan.meydan.request.Auth.Clan;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RespondToInvitationRequest {
    @NotNull
    private Boolean accept;
    private String reason;
}
