package com.meydan.meydan.service;

import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.exception.ErrorCode;
import com.meydan.meydan.models.entities.ActiveDoping;
import com.meydan.meydan.models.entities.DopingPackage;
import com.meydan.meydan.repository.ActiveDopingRepository;
import com.meydan.meydan.repository.DopingPackageRepository;
import com.meydan.meydan.request.Doping.DopingPurchaseRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DopingPurchaseService {

    private static final Logger logger = LoggerFactory.getLogger(DopingPurchaseService.class);

    private final DopingPackageRepository dopingPackageRepository;
    private final ActiveDopingRepository activeDopingRepository;
    private final WalletService walletService; // Bağımsız cüzdan servisimizi dahil ettik

    @Transactional
    public ActiveDoping buyDoping(Long userId, DopingPurchaseRequest request) {

        // 1. Satın alınmak istenen DopingPackage paketini çek
        DopingPackage dopingPackage = dopingPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new BaseException(
                        ErrorCode.VAL_001, 
                        "Doping paketi bulunamadı.", 
                        HttpStatus.NOT_FOUND, ""));

        if (!dopingPackage.getIsActive()) {
            throw new BaseException(ErrorCode.VAL_001, "Bu doping paketi artık satılmamaktadır.", HttpStatus.BAD_REQUEST, "");
        }

        if (dopingPackage.getTargetType() != request.getTargetType()) {
            throw new BaseException(ErrorCode.VAL_001, "Bu doping paketi seçili hedefe (" + request.getTargetType() + ") uygulanamaz.", HttpStatus.BAD_REQUEST, "");
        }

        // Fiyatı Double'dan BigDecimal'a güvenli çevir
        BigDecimal price = BigDecimal.valueOf(dopingPackage.getPrice());
        String purchaseDescription = dopingPackage.getName() + " paketi satın alındı. Hedef: " + request.getTargetType() + " ID: " + request.getTargetId();

        // 2. KRİTİK KONTROL ve İŞLEM: Harcamayı WalletService'e devret
        // Eğer bakiye yetersizse InSufficientBalanceException fırlatacak ve işlem iptal (Rollback) olacak.
        walletService.processRealPurchase(userId, price, purchaseDescription);

        // 3. Bakiye yeterliydi ve para kesildi, şimdi ActiveDoping tablosuna yeni kaydı ekle
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusHours(dopingPackage.getDurationHours());

        ActiveDoping activeDoping = new ActiveDoping();
        activeDoping.setUserId(userId);
        activeDoping.setTargetId(request.getTargetId());
        activeDoping.setTargetType(request.getTargetType());
        activeDoping.setDopingPackage(dopingPackage);
        activeDoping.setStartDate(now);
        activeDoping.setEndDate(endDate);
        activeDoping.setIsActive(true);

        logger.info("Doping Başarıyla Satın Alındı: Kullanıcı ID={}, Paket={}, Ödenen Tutar={}", 
                    userId, dopingPackage.getName(), price);

        return activeDopingRepository.save(activeDoping);
    }
}
