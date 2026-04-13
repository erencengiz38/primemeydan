package com.meydan.meydan.request.Turnuva;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GroupScoreListRequest {
    @NotEmpty(message = "Puan listesi boş olamaz")
    @Valid
    private List<GroupScoreRequest> scores;
}
