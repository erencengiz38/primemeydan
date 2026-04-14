package com.meydan.meydan.config;

import com.meydan.meydan.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // KANKA 1: CorsConfig sınıfını Security'e tanıtıyoruz ki Next.js'i düşman sanmasın
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // KANKA 2: ASIL ÇÖZÜM BURASI! "/auth/**" yerine "/api/auth/**" yaptık.
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        // Turnuva ve Klan listelemeleri herkese açık (Public) yapıldı
                        .requestMatchers("/api/turnuva/list", "/api/turnuva/list/paginated").permitAll()
                        .requestMatchers("/api/clan/list", "/api/clan/list/paginated", "/api/clan/{clanId}").permitAll()
                        // Diğer genel public izinler
                        .requestMatchers("/api/auth/**", "/api/organizations/**", "/api/tournaments/**", "/api/teams/**", "/api/categories/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
