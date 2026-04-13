package com.meydan.meydan.controller;

import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.dto.response.TournamentGroupScoreResponseDTO;
import com.meydan.meydan.models.entities.TournamentGroup;
import com.meydan.meydan.models.entities.TournamentGroupScore;
import com.meydan.meydan.models.entities.TournamentStage;
import com.meydan.meydan.request.Turnuva.CreateGroupRequest;
import com.meydan.meydan.request.Turnuva.CreateStageRequest;
import com.meydan.meydan.request.Turnuva.GroupScoreListRequest;
import com.meydan.meydan.service.TournamentMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tournament-matchings")
@Tag(name = "Tournament Matching", description = "Turnuva Eşleştirme, Aşama/Grup ve Lobi Puanlama API endpoint'leri")
@RequiredArgsConstructor
public class TournamentMatchingController {

    private final TournamentMatchingService matchingService;
    private final ModelMapper modelMapper;

    @PostMapping("/{turnuvaId}/stages")
    @Operation(summary = "Turnuvaya Aşama Ekle (Organizasyon Yetkilisi)", description = "Turnuvaya 'Ön Eleme', 'Grup Aşaması' gibi aşamalar ekler. Sadece turnuvayı düzenleyen organizasyonun yetkilileri yapabilir.")
    public ResponseEntity<ApiResponse<TournamentStage>> createStage(
            @PathVariable Long turnuvaId,
            @Valid @RequestBody CreateStageRequest request) {

        TournamentStage stage = matchingService.createStage(turnuvaId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Aşama başarıyla oluşturuldu.", stage));
    }

    @PostMapping("/stages/{stageId}/groups")
    @Operation(summary = "Aşamaya Grup Ekle (Organizasyon Yetkilisi)", description = "Aşamanın altına 'A Grubu', 'B Grubu' gibi gruplar ekler. Sadece turnuvayı düzenleyen organizasyonun yetkilileri yapabilir.")
    public ResponseEntity<ApiResponse<TournamentGroup>> createGroup(
            @PathVariable Long stageId,
            @Valid @RequestBody CreateGroupRequest request) {

        TournamentGroup group = matchingService.createGroup(stageId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grup başarıyla oluşturuldu.", group));
    }

    @PostMapping("/{turnuvaId}/stages/{stageId}/distribute")
    @Operation(summary = "Başvuruları Gruplara Dağıt (AYRIŞTIR BUTONU) (Organizasyon Yetkilisi)", description = "Turnuvada 'Kabul Edilmiş' olan tüm başvuruları, belirtilen aşamanın altındaki gruplara rastgele dağıtır.")
    public ResponseEntity<ApiResponse<Void>> distributeApplications(
            @PathVariable Long turnuvaId,
            @PathVariable Long stageId) {

        matchingService.distributeApplicationsToGroups(turnuvaId, stageId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Başvurular başarıyla gruplara (lobilere) dağıtıldı.", null));
    }

    // --- LOBİ SKOR GİRİŞİ VE GETİRİLMESİ ---

    @PostMapping("/{turnuvaId}/groups/{groupId}/scores")
    @Operation(summary = "Lobi (Grup) Puanlarını Gir / Güncelle (Organizasyon Yetkilisi)", description = "Organizatörün bir gruptaki (lobideki) takımların aldığı kill/sıralama puanlarını toplu olarak girmesini sağlar.")
    public ResponseEntity<ApiResponse<Void>> reportGroupScores(
            @PathVariable Long turnuvaId,
            @PathVariable Long groupId,
            @Valid @RequestBody GroupScoreListRequest request) {

        matchingService.reportGroupScores(turnuvaId, groupId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grup puanları başarıyla kaydedildi.", null));
    }

    @GetMapping("/groups/{groupId}/scores")
    @Operation(summary = "Lobi (Grup) Puan Durumunu Getir", description = "Bir gruptaki takımların puan durumunu en yüksek puandan en düşüğe sıralı şekilde getirir.")
    public ResponseEntity<ApiResponse<List<TournamentGroupScoreResponseDTO>>> getGroupScores(
            @PathVariable Long groupId) {

        List<TournamentGroupScore> scores = matchingService.getGroupScores(groupId);
        List<TournamentGroupScoreResponseDTO> dtoList = scores.stream()
                .map(score -> {
                    TournamentGroupScoreResponseDTO dto = modelMapper.map(score, TournamentGroupScoreResponseDTO.class);
                    if (score.getApplication() != null && score.getApplication().getClan() != null) {
                        dto.setClanName(score.getApplication().getClan().getName());
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(true, "Puan durumu getirildi.", dtoList));
    }
}
