package com.meydan.meydan.service;

import com.meydan.meydan.dto.Auth.AuthResponse;
import com.meydan.meydan.models.entities.Wallet;
import com.meydan.meydan.repository.WalletRepository;
import com.meydan.meydan.request.Auth.LoginRequest;
import com.meydan.meydan.request.Auth.RegisterRequest;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.repository.UserRepository;
import com.meydan.meydan.util.XssSanitizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository; // Cüzdan için eklendi
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final XssSanitizer xssSanitizer;
    private final JwtService jwtService;

    public AuthService(JwtService jwt, UserRepository userRepository, WalletRepository walletRepository, BCryptPasswordEncoder bCryptPasswordEncoder, XssSanitizer xssSanitizer) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.xssSanitizer = xssSanitizer;
        this.jwtService = jwt;
    }

    @Transactional
    public String register(RegisterRequest request){
        if(userRepository.existsByMail(request.getMail())){
            throw new RuntimeException("Bu mail adresi zaten kullanılıyor.");
        }

        // XSS koruması
        String sanitizedMail = xssSanitizer.sanitizeBasic(request.getMail());
        String sanitizedDisplayName = xssSanitizer.sanitizeAndLimit(request.getDisplay_name(), 100);
        String sanitizedTag = xssSanitizer.sanitizeAndLimit(request.getTag(), 50);

        User user = new User();
        user.setMail(sanitizedMail);
        user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        user.setDisplay_name(sanitizedDisplayName);
        user.setTag(sanitizedTag);
        
        // Önce kullanıcıyı kaydet ve ID'sini al
        User savedUser = userRepository.save(user);
        
        // YENİ: Kullanıcı için boş bir cüzdan oluştur
        Wallet newWallet = new Wallet();
        newWallet.setUserId(savedUser.getId());
        newWallet.setRealBalance(BigDecimal.ZERO);
        newWallet.setMeydanCoin(BigDecimal.ZERO);
        walletRepository.save(newWallet);
        
        return "Kayıt başarılı";
    }

    public AuthResponse login(LoginRequest request){
        String sanitizedMail = xssSanitizer.sanitizeBasic(request.getMail());
        User user = userRepository.findByMail(sanitizedMail).orElse(null);

        if(user == null){
            return new AuthResponse(null, "Kullanıcı bulunamadı.");
        }

        boolean matches = bCryptPasswordEncoder.matches(request.getPassword(), user.getPassword());
        if(!matches){
            return new AuthResponse(null, "Hatalı şifre.");
        }

        String token = jwtService.generateToken(user.getMail());
        return new AuthResponse(token, "Başarılı");
    }
}
