package com.meydan.meydan.request.Turnuva;

import com.meydan.meydan.models.enums.ParticipantType;
import com.meydan.meydan.models.enums.TournamentFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Date;

@Data
public class AddTurnuvaRequestBody {
    @NotNull(message = "Kategori ID boş bırakılamaz")
    private Long categoryId;

    @NotBlank(message = "Başlık boş bırakılamaz")
    private String title;

    @NotBlank(message = "Açıklama boş bırakılamaz")
    private String description;

    @NotNull(message = "Başlangıç tarihi gereklidir")
    private Date start_date;

    @NotNull(message = "Bitiş tarihi gereklidir")
    private Date finish_date;

    @NotNull(message = "Kayıt kapanış tarihi gereklidir")
    private Date registrationDeadline;

    @NotNull(message = "Maksimum katılımcı sayısı gereklidir")
    private Integer maxParticipants;

    private Integer minParticipants = 2;

    @NotNull(message = "Takım boyutu (teamSize) gereklidir")
    private Integer teamSize;

    @NotNull(message = "Maç kapasitesi (matchCapacity) gereklidir")
    private Integer matchCapacity;

    @NotNull(message = "Katılımcı tipi (SOLO/CLAN) gereklidir")
    private ParticipantType participantType;

    @NotNull(message = "Turnuva formatı (SCRIM/STAGE_BASED) gereklidir")
    private TournamentFormat tournamentFormat;

    private String imageUrl;
    private Double reward_amount;
    private String reward_currency;
    private String player_format;

}