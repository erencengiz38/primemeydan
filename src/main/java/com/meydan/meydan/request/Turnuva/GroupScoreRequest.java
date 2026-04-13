package com.meydan.meydan.request.Turnuva;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupScoreRequest {
    @NotNull(message = "Başvuru ID boş olamaz")
    private Long applicationId;

    @NotNull(message = "Puan boş olamaz")
    private Integer score;

    private Integer placement;
    private Boolean isAdvanced;
}
