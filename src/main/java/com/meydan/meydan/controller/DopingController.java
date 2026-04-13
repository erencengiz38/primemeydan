package com.meydan.meydan.controller;

import com.meydan.meydan.config.CurrentUserId;
import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.models.entities.ActiveDoping;
import com.meydan.meydan.models.entities.DopingPackage;
import com.meydan.meydan.models.enums.TargetType;
import com.meydan.meydan.request.Doping.PurchaseDopingRequest;
import com.meydan.meydan.service.DopingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doping")
@Tag(name = "Doping", description = "Doping (Öne Çıkarma) API endpoint'leri")
@RequiredArgsConstructor
public class DopingController {

    private final DopingService dopingService;

    // --- MAĞAZA İŞLEMLERİ ---
    
    @GetMapping("/packages")
    @Operation(summary = "Tüm aktif doping paketlerini listele")
    public ResponseEntity<ApiResponse<List<DopingPackage>>> getAllPackages() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Doping paketleri getirildi", dopingService.getAllActivePackages()));
    }

    @GetMapping("/packages/{targetType}")
    @Operation(summary = "Belirli bir hedefe (CLAN, TOURNAMENT vs.) uygun doping paketlerini listele")
    public ResponseEntity<ApiResponse<List<DopingPackage>>> getPackagesByTargetType(@PathVariable TargetType targetType) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Hedefe uygun paketler getirildi", dopingService.getPackagesByTargetType(targetType)));
    }

    // --- SATIN ALMA VE İPTAL İŞLEMLERİ ---

    @PostMapping("/purchase")
    @Operation(summary = "Doping Satın Al ve Uygula", description = "Belirtilen hedef ID'sine yetkili olduğunuz doğrulanır ve doping uygulanır.")
    public ResponseEntity<ApiResponse<ActiveDoping>> purchaseDoping(
            @Valid @RequestBody PurchaseDopingRequest request,
            @CurrentUserId Long userId) {

        ActiveDoping activeDoping = dopingService.purchaseDoping(userId, request.getTargetId(), request.getTargetType(), request.getPackageId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Doping başarıyla satın alındı ve hedefe uygulandı", activeDoping));
    }

    @DeleteMapping("/{dopingId}/cancel")
    @Operation(summary = "Aktif Dopingi İptal Et", description = "Satın alınmış bir dopingi süresi dolmadan pasife çeker.")
    public ResponseEntity<ApiResponse<Void>> cancelDoping(
            @PathVariable Long dopingId,
            @CurrentUserId Long userId) {

        dopingService.cancelDoping(dopingId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doping başarıyla iptal edildi", null));
    }

    // --- SORGULAMA İŞLEMLERİ ---

    @GetMapping("/target/{targetType}/{targetId}")
    @Operation(summary = "Bir klanın/turnuvanın aktif dopinglerini getir", description = "Frontend'de nesnenin üstünde VIP rozeti vb. göstermek için kullanılır.")
    public ResponseEntity<ApiResponse<List<ActiveDoping>>> getActiveDopingsForTarget(
            @PathVariable TargetType targetType,
            @PathVariable Long targetId) {

        return ResponseEntity.ok(new ApiResponse<>(true, "Hedefin aktif dopingleri getirildi", dopingService.getActiveDopingsForTarget(targetType, targetId)));
    }

    @GetMapping("/my-purchases")
    @Operation(summary = "Kendi satın aldığım aktif dopingleri listele")
    public ResponseEntity<ApiResponse<List<ActiveDoping>>> getMyActivePurchases(@CurrentUserId Long userId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Kendi satın aldığınız dopingler getirildi", dopingService.getUserActivePurchases(userId)));
    }

    // --- ADMİN İŞLEMLERİ ---
    
    @PostMapping("/admin/packages/create")
    @Operation(summary = "Yeni bir Doping Paketi oluştur (Sadece Sistem Yöneticisi)")
    @PreAuthorize("hasRole('ADMIN')") // MADDE 3: Admin yetkisi eklendi
    public ResponseEntity<ApiResponse<DopingPackage>> createDopingPackage(
            @Valid @RequestBody DopingPackage dopingPackage) {
            
        DopingPackage createdPackage = dopingService.createDopingPackage(dopingPackage);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Doping paketi mağazaya eklendi", createdPackage));
    }
}
