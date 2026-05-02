package com.meydan.meydan.service;

import com.meydan.meydan.dto.Auth.AuthResponse;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.models.entities.Wallet;
import com.meydan.meydan.models.enums.Role;
import com.meydan.meydan.repository.UserRepository;
import com.meydan.meydan.repository.WalletRepository;
import com.meydan.meydan.request.Auth.AdminRequest;
import com.meydan.meydan.request.Auth.LoginRequest;
import com.meydan.meydan.request.Auth.RegisterRequest;
import com.meydan.meydan.util.XssSanitizer;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.admin.mail:admin@meydan.com}")
    private String adminMail;

    @Value("${app.admin.password:123}")
    private String adminPassword;

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

        User user = new User();
        user.setMail(sanitizedMail);
        user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        user.setDisplay_name(sanitizedDisplayName);
        user.setTag(""); 
        user.setRole(Role.USER); 
        user.setDevicePlatform(request.getDevicePlatform());

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

        String roleName = user.getRole() != null ? user.getRole().name() : Role.USER.name();
        String token = jwtService.generateToken(user.getId().toString(), user.getMail(), roleName);

        return new AuthResponse(token, "Başarılı");
    }

    public AuthResponse adminLogin(AdminRequest request) {
        String sanitizedMail = xssSanitizer.sanitizeBasic(request.getMail());

        if (!adminMail.equals(sanitizedMail)) {
            return new AuthResponse(null, "Admin kullanıcısı bulunamadı.");
        }

        if (!adminPassword.equals(request.getPassword())) {
            return new AuthResponse(null, "Hatalı şifre.");
        }

        // Admin logini başarılı, token içine ADMIN rolünü gömüyoruz. 
        // ID olarak "0" veriyoruz (çünkü DB'de karşılığı olmayabilir, properties'den çekiyoruz)
        String token = jwtService.generateToken("0", adminMail, Role.ADMIN.name());

        return new AuthResponse(token, "Başarılı");
    }
}
