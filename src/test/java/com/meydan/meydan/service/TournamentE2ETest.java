package com.meydan.meydan.service;

import com.meydan.meydan.exception.TournamentFullException;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.*;
import com.meydan.meydan.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentE2ETest {

    // BAK AGA BURAYI SENİN KLASÖRDEKİ İSİMLERE (TÜRKÇE) ÇEVİRDİM!
    @Mock private TurnuvaRepository turnuvaRepository;
    @Mock private WalletService walletService;
    @Mock private TournamentApplicationRepository applicationRepository;

    @InjectMocks
    private TurnuvaService turnuvaService;

    @Test
    @DisplayName("Kapasite Sınırı Testi: 17 Kişilik Turnuvaya 18. Kişi Girememeli")
    void testTournamentCapacityLimit() {
        Turnuva tournament = new Turnuva();
        tournament.setId(1L);
        tournament.setMaxParticipants(17);
        tournament.setCurrentParticipantsCount(17);

        when(turnuvaRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThrows(TournamentFullException.class, () -> {
            turnuvaService.applyToTournament(1L, 999L);
        });

        System.out.println("✅ Kapasite testi başarılı: 18. başvuru reddedildi!");
    }

    @Test
    @DisplayName("Ödül Dağıtım Testi: Kazananlara %60, %30, %10 Oranında Coin Yatmalı")
    void testRewardDistribution() {
        Long tournamentId = 1L;
        BigDecimal totalPool = new BigDecimal("10000.0000");

        Tournament tournament = new Tournament();
        tournament.setId(tournamentId);
        tournament.setTotalPrizePool(totalPool);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        when(turnuvaRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        turnuvaService.finishTournamentAndDistribute(tournamentId, 10L, 20L, 30L);

        verify(walletService, times(1)).rewardMeydanCoin(eq(10L), eq(new BigDecimal("6000.0000")), anyString());
        verify(walletService, times(1)).rewardMeydanCoin(eq(20L), eq(new BigDecimal("3000.0000")), anyString());
        verify(walletService, times(1)).rewardMeydanCoin(eq(30L), eq(new BigDecimal("1000.0000")), anyString());

        System.out.println("✅ Ödül dağıtım testi başarılı: Coinler doğru hesaplandı!");
    }
}