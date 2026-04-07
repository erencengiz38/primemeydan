package com.meydan.meydan.request.Auth.Turnuva;

import com.meydan.meydan.models.entities.TournamentApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateApplicationStatusRequestBody {

    @NotNull(message = "Yeni durum boş olamaz")
    private TournamentApplicationStatus status;

    @Size(max = 500, message = "Reddetme sebebi 500 karakterden uzun olamaz")
    private String rejectionReason;
}
