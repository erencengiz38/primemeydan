package com.meydan.meydan.service;

import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.exception.ErrorCode;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.ClanMemberRole;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.models.enums.TargetType;
import com.meydan.meydan.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DopingService {

    private static final Logger logger = LoggerFactory.getLogger(DopingService.class);

    private final DopingPackageRepository dopingPackageRepository;
    private final ActiveDopingRepository activeDopingRepository;

    // Yetki kontrolü için gereken repolar
    private final ClanMemberRepository clanMemberRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final TurnuvaRepository turnuvaRepository;

    // --- Mağaza: Aktif Paketleri Getir ---
    public List<DopingPackage> getAllActivePackages() {
        return dopingPackageRepository.findByIsActiveTrue();
    }

    public List<DopingPackage> getPackagesByTargetType(TargetType targetType) {
        return dopingPackageRepository.findByTargetTypeAndIsActiveTrue(targetType);
    }

    // --- Admin: Yeni Doping Paketi Oluştur ---
    @Transactional
    public DopingPackage createDopingPackage(DopingPackage dopingPackage) {
        return dopingPackageRepository.save(dopingPackage);
    }

    // --- Yetki (BOLA) Kontrolü ---
    private void checkTargetPermission(Long userId, Long targetId, TargetType targetType) {
        boolean hasPermission = false;

        switch (targetType) {
            case CLAN:
                hasPermission = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(targetId, userId)
                        .map(m -> m.getRole() == ClanMemberRole.OWNER || m.getRole() == ClanMemberRole.MANAGER)
                        .orElse(false);
                if (!hasPermission) throw new BaseException(ErrorCode.AUTH_001, "Bu klana doping basmak için yetkiniz yok (Owner veya Manager olmalısınız).", HttpStatus.FORBIDDEN, "");
                break;
            case ORGANIZATION:
                hasPermission = organizationMembershipRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                        targetId, userId, Arrays.asList(OrganizationRole.OWNER, OrganizationRole.ADMIN));
                if (!hasPermission) throw new BaseException(ErrorCode.AUTH_001, "Bu organizasyona doping basmak için yetkiniz yok.", HttpStatus.FORBIDDEN, "");
                break;
            case TOURNAMENT:
                Turnuva turnuva = turnuvaRepository.findById(targetId)
                        .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Turnuva bulunamadı.", HttpStatus.NOT_FOUND, ""));
                hasPermission = organizationMembershipRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                        turnuva.getOrganizationId(), userId, Arrays.asList(OrganizationRole.OWNER, OrganizationRole.ADMIN));
                if (!hasPermission) throw new BaseException(ErrorCode.AUTH_001, "Bu turnuvaya doping basmak için organizasyon yetkilisi olmalısınız.", HttpStatus.FORBIDDEN, "");
                break;
            case LISTING:
                // İleride İlan/Listing eklendiğinde burası dolacak
                // hasPermission = listingRepository.existsByIdAndUserId(targetId, userId);
                break;
        }
    }

    // --- Kullanıcı: Doping Satın Al ve Uygula ---
    @Transactional
    public ActiveDoping purchaseDoping(Long userId, Long targetId, TargetType targetType, Long packageId) {
        
        // 1. Yetki Kontrolü: Bu adam bu nesneye doping basabilir mi?
        checkTargetPermission(userId, targetId, targetType);

        // 2. Paket Kontrolü
        DopingPackage dopingPackage = dopingPackageRepository.findById(packageId)
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Doping paketi bulunamadı", HttpStatus.NOT_FOUND, ""));

        if (!dopingPackage.getIsActive()) {
            throw new BaseException(ErrorCode.VAL_001, "Bu doping paketi artık satılmamaktadır.", HttpStatus.BAD_REQUEST, "");
        }

        if (dopingPackage.getTargetType() != targetType) {
            throw new BaseException(ErrorCode.VAL_001, "Bu doping paketi seçili hedefe uygulanamaz. Hedef tipi: " + targetType, HttpStatus.BAD_REQUEST, "");
        }

        // TODO: Bakiye (Coin/TL) düşüm işlemi burada yapılmalıdır.
        // UserWallet wallet = walletService.findByUserId(userId);
        // walletService.deductBalance(userId, dopingPackage.getPrice());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusHours(dopingPackage.getDurationHours());

        ActiveDoping activeDoping = new ActiveDoping();
        activeDoping.setUserId(userId);
        activeDoping.setTargetId(targetId);
        activeDoping.setTargetType(targetType);
        activeDoping.setDopingPackage(dopingPackage);
        activeDoping.setStartDate(now);
        activeDoping.setEndDate(endDate);
        activeDoping.setIsActive(true);

        logger.info("YENİ DOPİNG: Kullanıcı ID={}, Hedef ID={}, Hedef Tipi={}, Paket ID={}", userId, targetId, targetType, packageId);
        return activeDopingRepository.save(activeDoping);
    }

    // --- Kullanıcı: Dopingi İptal Et (Sil) ---
    @Transactional
    public void cancelDoping(Long dopingId, Long userId) {
        ActiveDoping doping = activeDopingRepository.findById(dopingId)
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Aktif doping bulunamadı", HttpStatus.NOT_FOUND, ""));

        // Dopingi satın alan kişi veya hedef objenin sahibi iptal edebilir
        if (!doping.getUserId().equals(userId)) {
            // Belki objenin diğer bir adminidir diye tekrar yetki kontrolü yapalım
            checkTargetPermission(userId, doping.getTargetId(), doping.getTargetType());
        }

        doping.setIsActive(false);
        activeDopingRepository.save(doping);
        logger.info("DOPİNG İPTAL EDİLDİ: Doping ID={}, İptal Eden Kullanıcı ID={}", dopingId, userId);
    }

    // --- Okuma: Bir Hedefin Aktif Dopinglerini Getir ---
    public List<ActiveDoping> getActiveDopingsForTarget(TargetType targetType, Long targetId) {
        return activeDopingRepository.findByTargetTypeAndTargetIdAndIsActiveTrue(targetType, targetId);
    }

    // --- Okuma: Kullanıcının Satın Aldığı Dopingleri Getir ---
    public List<ActiveDoping> getUserActivePurchases(Long userId) {
        return activeDopingRepository.findByUserIdAndIsActiveTrue(userId);
    }

    // --- Zamanlanmış Görev (Scheduled Task): Süresi Bitenleri Otomatik Kapat ---
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deactivateExpiredDopingsTask() {
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = activeDopingRepository.deactivateExpiredDopings(now);
        if (updatedCount > 0) {
            logger.info("⏰ Zamanlanmış Görev: Süresi dolan {} adet doping pasife çekildi.", updatedCount);
        }
    }
}
