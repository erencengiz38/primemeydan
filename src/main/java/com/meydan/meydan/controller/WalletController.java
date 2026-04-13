package com.meydan.meydan.controller;

import com.meydan.meydan.config.CurrentUserId;
import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.dto.response.WalletResponseDTO;
import com.meydan.meydan.dto.response.WalletTransactionResponseDTO;
import com.meydan.meydan.models.entities.Wallet;
import com.meydan.meydan.models.entities.WalletTransaction;
import com.meydan.meydan.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallets")
@Tag(name = "Wallet", description = "Cüzdan ve Finans API endpoint'leri")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final ModelMapper modelMapper;

    @GetMapping("/me")
    @Operation(summary = "Cüzdanımı Getir", description = "Kullanıcının güncel TL ve Meydan Coin bakiyesini döner.")
    public ResponseEntity<ApiResponse<WalletResponseDTO>> getMyWallet(@CurrentUserId Long userId) {
        Wallet wallet = walletService.getOrCreateWallet(userId);
        WalletResponseDTO responseDTO = modelMapper.map(wallet, WalletResponseDTO.class);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cüzdan bilgileri getirildi", responseDTO));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Hesap Hareketlerini Getir", description = "Kullanıcının cüzdanında gerçekleşen tüm harcama, kazanma ve yükleme geçmişini döner.")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponseDTO>>> getMyTransactions(@CurrentUserId Long userId) {
        List<WalletTransaction> transactions = walletService.getWalletTransactions(userId);
        
        List<WalletTransactionResponseDTO> responseDTOs = transactions.stream()
                .map(t -> modelMapper.map(t, WalletTransactionResponseDTO.class))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(new ApiResponse<>(true, "Hesap hareketleri getirildi", responseDTOs));
    }
}
