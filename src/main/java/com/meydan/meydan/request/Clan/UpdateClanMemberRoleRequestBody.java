package com.meydan.meydan.request.Clan;

import com.meydan.meydan.models.enums.ClanMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateClanMemberRoleRequestBody {
    @NotNull(message = "Clan üyesi ID'si boş olamaz")
    private Long clanMemberId;

    @NotNull(message = "Yeni rol boş olamaz")
    private ClanMemberRole newRole;
}
