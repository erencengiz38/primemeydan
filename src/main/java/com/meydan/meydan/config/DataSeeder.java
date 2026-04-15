package com.meydan.meydan.config;

import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.models.enums.Role;
import com.meydan.meydan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.admin.mail:admin@meydan.com}")
    private String adminMail;

    @Value("${app.admin.password:123}")
    private String adminPassword;

    public DataSeeder(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = new User();
            admin.setMail(adminMail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setDisplay_name("Admin");
            admin.setRole(Role.ADMIN);
            admin.setTag("ADMIN");
            userRepository.save(admin);
            System.out.println("Varsayilan Admin hesabi olusturuldu!");
        }
    }
}