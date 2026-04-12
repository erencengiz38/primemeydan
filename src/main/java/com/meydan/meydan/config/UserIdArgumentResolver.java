package com.meydan.meydan.config;

import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class UserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BaseException(
                    ErrorCode.AUTH_002,
                    "Kullanıcı oturumu bulunamadı. Lütfen giriş yapın.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        try {
            // Principal artık mail değil, doğrudan ID (String formatında)
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BaseException(
                    ErrorCode.AUTH_003,
                    "Kullanıcı kimliği doğrulanamadı. Geçersiz token formatı.",
                    HttpStatus.UNAUTHORIZED,
                    "Principal: " + authentication.getName()
            );
        }
    }
}
