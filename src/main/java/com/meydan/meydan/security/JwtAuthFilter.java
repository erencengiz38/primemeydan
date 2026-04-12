package com.meydan.meydan.security;

import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.repository.UserRepository;
import com.meydan.meydan.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String mail = jwtService.extractMail(token);

        if (mail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userRepository.findByMail(mail).orElse(null);

            if (user != null && jwtService.isTokenValid(token, user.getMail())) {
                // BURAYI DEĞİŞTİRDİK:
                // İlk parametreye user.getMail() yerine user.getId().toString() veriyoruz.
                // Artık sistemin her yerinde principal.getName() dediğinde direkt sayı (ID) dönecek.
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user.getId().toString(), // ARTIK BURASI ID!
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getTag()))
                        );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}