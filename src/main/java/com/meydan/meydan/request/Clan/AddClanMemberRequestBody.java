package com.meydan.meydan.request.Clan;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddClanMemberRequestBody {

    @NotNull(message = "Clan ID boş olamaz")
    private Long clanId;

    @NotNull(message = "User ID boş olamaz")
    private Long userId;
}
