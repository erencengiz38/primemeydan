package com.meydan.meydan.service;

import com.meydan.meydan.dto.Auth.AuthResponse;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.models.entities.Wallet;
import com.meydan.meydan.models.enums.Role;
import com.meydan.meydan.repository.UserRepository;
import com.meydan.meydan.repository.WalletRepository;
import com.meydan.meydan.request.Auth.LoginRequest;
import com.meydan.meydan.request.Auth.RegisterRequest;
import com.meydan.meydan.util.XssSanitizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final XssSanitizer xssSanitizer;
    private final JwtService jwtService;

    public AuthService(
            JwtService jwtService,
            UserRepository userRepository,
            WalletRepository walletRepository,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            XssSanitizer xssSanitizer
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.xssSanitizer = xssSanitizer;
        this.jwtService = jwtService;
    }

    @Transactional
    public String register(RegisterRequest request) {
        String sanitizedMail = xssSanitizer.sanitizeBasic(request.getMail());

        if (userRepository.existsByMail(sanitizedMail)) {
            throw new RuntimeException("Bu mail adresi zaten kullanılıyor.");
        }

        String sanitizedDisplayName = xssSanitizer.sanitizeAndLimit(request.getDisplay_name(), 100);
        // Güvenlik: tag dışarıdan alınmıyor, profil etiketi olarak sabit veya boş bırakılıyor.
        // Rol kesinlikle USER olarak atanıyor.

        User user = new User();
        user.setMail(sanitizedMail);
        user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        user.setDisplay_name(sanitizedDisplayName);
        user.setTag(""); // Kullanıcının dışarıdan "ADMIN" vb. göndermesini engellemek için
        user.setRole(Role.USER); // Varsayılan olarak her kayıt olan USER'dır.

        User savedUser = userRepository.save(user);

        Wallet newWallet = new Wallet();
        newWallet.setUserId(savedUser.getId());
        newWallet.setRealBalance(BigDecimal.ZERO);
        newWallet.setMeydanCoin(BigDecimal.ZERO);
        walletRepository.save(newWallet);

        return "Kayıt başarılı";
    }

    public AuthResponse login(LoginRequest request) {
        String sanitizedMail = xssSanitizer.sanitizeBasic(request.getMail());
        User user = userRepository.findByMail(sanitizedMail).orElse(null);

        if (user == null) {
            return new AuthResponse(null, "Kullanıcı bulunamadı.");
        }

        boolean matches = bCryptPasswordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!matches) {
            return new AuthResponse(null, "Hatalı şifre.");
        }

        // Rol token içine dahil ediliyor
        String roleName = user.getRole() != null ? user.getRole().name() : Role.USER.name();
        String token = jwtService.generateToken(user.getId().toString(), user.getMail(), roleName);

        return new AuthResponse(token, "Başarılı");
    }
}