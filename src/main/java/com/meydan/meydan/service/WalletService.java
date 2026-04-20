package com.meydan.meydan.service;

import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.exception.ErrorCode;
import com.meydan.meydan.exception.InSufficientBalanceException;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.models.entities.Wallet;
import com.meydan.meydan.models.entities.WalletTransaction;
import com.meydan.meydan.models.enums.CurrencyType;
import com.meydan.meydan.models.enums.TransactionType;
import com.meydan.meydan.repository.UserRepository;
import com.meydan.meydan.repository.WalletRepository;
import com.meydan.meydan.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createWalletsForExistingUsers() {
        logger.info("📦 Sistem Başlatıldı: Cüzdanı olmayan eski kullanıcılar taranıyor...");
        List<User> allUsers = userRepository.findAll();
        int createdWallets = 0;

        for (User user : allUsers) {
            if (walletRepository.findByUserId(user.getId()).isEmpty()) {
                Wallet newWallet = new Wallet();
                newWallet.setUserId(user.getId());
                newWallet.setRealBalance(BigDecimal.ZERO);
                newWallet.setMeydanCoin(BigDecimal.ZERO);
                walletRepository.save(newWallet);
                createdWallets++;
            }
        }

        if (createdWallets > 0) {
            logger.info("✅ Başarılı: {} adet eski kullanıcıya otomatik olarak yeni cüzdan (Wallet) tanımlandı.", createdWallets);
        } else {
            logger.info("✅ Cüzdan taraması tamamlandı. Tüm kullanıcıların zaten bir cüzdanı mevcut.");
        }
    }

    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet newWallet = new Wallet();
            newWallet.setUserId(userId);
            newWallet.setRealBalance(BigDecimal.ZERO);
            newWallet.setMeydanCoin(BigDecimal.ZERO);
            return walletRepository.save(newWallet);
        });
    }

    @Transactional
    public Wallet getOrCreateWalletForUpdate(Long userId) {
        return walletRepository.findByUserIdForUpdate(userId).orElseGet(() -> {
            Wallet newWallet = new Wallet();
            newWallet.setUserId(userId);
            newWallet.setRealBalance(BigDecimal.ZERO);
            newWallet.setMeydanCoin(BigDecimal.ZERO);
            return walletRepository.save(newWallet);
        });
    }

    public List<WalletTransaction> getWalletTransactions(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return walletTransactionRepository.findByWalletIdOrderByTransactionDateDesc(wallet.getId());
    }

    @Transactional
    public void processRealPurchase(Long userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BaseException(ErrorCode.VAL_001, "İşlem tutarı sıfırdan küçük olamaz.", HttpStatus.BAD_REQUEST, "");
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);

        if (wallet.getRealBalance().compareTo(amount) < 0) {
            logger.warn("Yetersiz TL Bakiyesi: Kullanıcı ID={}, Mevcut TL={}, İstenen={}", userId, wallet.getRealBalance(), amount);
            throw new InSufficientBalanceException("Gerçek TL bakiyeniz bu işlem için yetersiz. Lütfen bakiye yükleyiniz.");
        }

        wallet.setRealBalance(wallet.getRealBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(TransactionType.PURCHASE);
        transaction.setCurrencyType(CurrencyType.TRY);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionDate(LocalDateTime.now());
        walletTransactionRepository.save(transaction);

        logger.info("TL Harcaması Başarılı: Kullanıcı ID={}, Tutar={}, Açıklama={}", userId, amount, description);
    }

    @Transactional
    public void rewardMeydanCoin(Long userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BaseException(ErrorCode.VAL_001, "Ödül tutarı sıfırdan küçük olamaz.", HttpStatus.BAD_REQUEST, "");
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);

        wallet.setMeydanCoin(wallet.getMeydanCoin().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(TransactionType.COIN_REWARD);
        transaction.setCurrencyType(CurrencyType.MEYDAN_COIN);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionDate(LocalDateTime.now());
        walletTransactionRepository.save(transaction);

        logger.info("Meydan Coin Kazanıldı: Kullanıcı ID={}, Tutar={}, Açıklama={}", userId, amount, description);
    }

    @Transactional
    public void spendMeydanCoin(Long userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BaseException(ErrorCode.VAL_001, "Harcama tutarı sıfırdan küçük olamaz.", HttpStatus.BAD_REQUEST, "");
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);

        if (wallet.getMeydanCoin().compareTo(amount) < 0) {
            logger.warn("Yetersiz Meydan Coin: Kullanıcı ID={}, Mevcut Coin={}, İstenen={}", userId, wallet.getMeydanCoin(), amount);
            throw new InSufficientBalanceException("Meydan Coin bakiyeniz bu işlem için yetersiz.");
        }

        wallet.setMeydanCoin(wallet.getMeydanCoin().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(TransactionType.COIN_SPEND);
        transaction.setCurrencyType(CurrencyType.MEYDAN_COIN);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionDate(LocalDateTime.now());
        walletTransactionRepository.save(transaction);

        logger.info("Meydan Coin Harcandı: Kullanıcı ID={}, Tutar={}, Açıklama={}", userId, amount, description);
    }
}