package com.meydan.meydan.request.Auth.Clan;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteToClanRequest {
    @NotNull
    private Long userIdToInvite;
}
