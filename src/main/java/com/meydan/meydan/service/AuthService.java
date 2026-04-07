package com.meydan.meydan.service;


import com.meydan.meydan.dto.Auth.AuthResponse;
import com.meydan.meydan.request.Auth.LoginRequest;
import com.meydan.meydan.request.Auth.RegisterRequest;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.repository.UserRepository;
import com.meydan.meydan.util.XssSanitizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final XssSanitizer xssSanitizer;
    private JwtService jwtService;

    public AuthService(JwtService jwt, UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder, XssSanitizer xssSanitizer) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.xssSanitizer = xssSanitizer;
        this.jwtService = jwt;
    }

    public String register(RegisterRequest request){
        if(userRepository.existsByMail(request.getMail())){
            throw new RuntimeException("Bu mail adresi zaten kullanılıyor.");
        }

        // XSS koruması - input alanlarını temizle
        String sanitizedMail = xssSanitizer.sanitizeBasic(request.getMail());
        String sanitizedDisplayName = xssSanitizer.sanitizeAndLimit(request.getDisplay_name(), 100);
        String sanitizedTag = xssSanitizer.sanitizeAndLimit(request.getTag(), 50);

        // XSS saldırısı tespit edildi mi kontrol et
        if (xssSanitizer.containsXss(request.getMail()) ||
            xssSanitizer.containsXss(request.getDisplay_name()) ||
            xssSanitizer.containsXss(request.getTag())) {
            System.out.println("XSS saldırısı tespit edildi ve temizlendi. Kullanıcı kayıt işlemi.");
        }

        User user = new User();

        user.setMail(sanitizedMail);
        user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        user.setDisplay_name(sanitizedDisplayName);
        user.setTag(sanitizedTag);
        userRepository.save(user);
        
        return "Kayıt başarılı";
    }


    public AuthResponse login(LoginRequest request){

        // XSS koruması - mail alanını temizle
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
