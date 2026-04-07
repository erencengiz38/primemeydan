package com.meydan.meydan.util;

import org.springframework.stereotype.Component;

@Component
public class SocialMediaValidator {

    public enum SocialMediaType {
        INSTAGRAM("INSTAGRAM"),
        WHATSAPP("WHATSAPP"),
        DISCORD("DISCORD"),
        TELEGRAM("TELEGRAM");

        private final String type;

        SocialMediaType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    /**
     * Link'in geçerli bir sosyal ağ URL'si olup olmadığını kontrol eder
     */
    public boolean isValidSocialMediaUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true; // Link opsiyonel
        }

        String lowerUrl = url.toLowerCase().trim();
        
        return isInstagramUrl(lowerUrl) || 
               isWhatsappUrl(lowerUrl) || 
               isDiscordUrl(lowerUrl) || 
               isTelegramUrl(lowerUrl);
    }

    /**
     * Instagram URL'si mi kontrol eder
     */
    private boolean isInstagramUrl(String url) {
        return url.contains("instagram.com") || 
               url.contains("ig.me") ||
               url.matches(".*@?[a-zA-Z0-9_.]*instagram.*");
    }

    /**
     * WhatsApp URL'si mi kontrol eder
     */
    private boolean isWhatsappUrl(String url) {
        return url.contains("whatsapp.com") || 
               url.contains("wa.me") ||
               url.contains("api.whatsapp.com");
    }

    /**
     * Discord URL'si mi kontrol eder
     */
    private boolean isDiscordUrl(String url) {
        return url.contains("discord.com") || 
               url.contains("discord.gg") ||
               url.contains("discordapp.com");
    }

    /**
     * Telegram URL'si mi kontrol eder
     */
    private boolean isTelegramUrl(String url) {
        return url.contains("t.me") || 
               url.contains("telegram.me") ||
               url.contains("telegram.org");
    }

    /**
     * Link'in sosyal ağ türünü otomatik belirler
     */
    public SocialMediaType detectSocialMediaType(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        String lowerUrl = url.toLowerCase().trim();

        if (isInstagramUrl(lowerUrl)) {
            return SocialMediaType.INSTAGRAM;
        } else if (isWhatsappUrl(lowerUrl)) {
            return SocialMediaType.WHATSAPP;
        } else if (isDiscordUrl(lowerUrl)) {
            return SocialMediaType.DISCORD;
        } else if (isTelegramUrl(lowerUrl)) {
            return SocialMediaType.TELEGRAM;
        }

        return null;
    }

    /**
     * Link'in URL formatında olup olmadığını temel olarak kontrol eder
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }

        try {
            new java.net.URL(url);
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }
}

