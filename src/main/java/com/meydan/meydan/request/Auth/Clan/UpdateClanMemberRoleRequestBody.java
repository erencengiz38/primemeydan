package com.meydan.meydan.request.Auth.Clan;

import com.meydan.meydan.models.entities.ClanMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateClanMemberRoleRequestBody {

    @NotNull(message = "Clan member ID boş olamaz")
    private Long clanMemberId;

    @NotNull(message = "Yeni rol boş olamaz")
    private ClanMemberRole newRole;
}
