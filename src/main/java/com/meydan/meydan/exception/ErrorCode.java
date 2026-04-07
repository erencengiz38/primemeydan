package com.meydan.meydan.exception;

public enum ErrorCode {
    // Validation errors (VAL_XXX)
    VAL_001("VAL_001", "Zorunlu alan boş bırakılamaz"),
    VAL_002("VAL_002", "Tarih formatı hatalı"),
    VAL_003("VAL_003", "Başlangıç tarihi bitiş tarihinden sonra olamaz"),
    
    // Turnuva errors (TRN_XXX)
    TRN_001("TRN_001", "Turnuva bulunamadı"),
    TRN_002("TRN_002", "Turnuva oluşturulurken hata oluştu"),
    TRN_003("TRN_003", "Turnuva güncellenirken hata oluştu"),
    TRN_004("TRN_004", "Turnuva silinirken hata oluştu"),
    TRN_005("TRN_005", "Turnuva listelenirken hata oluştu"),
    
    // Link validation errors (LINK_XXX)
    LINK_001("LINK_001", "Geçersiz sosyal ağ URL'si - Instagram, WhatsApp, Discord veya Telegram kullanınız"),
    LINK_002("LINK_002", "URL formatı hatalı"),
    LINK_003("LINK_003", "Desteklenmeyen sosyal ağ - sadece Instagram, WhatsApp, Discord ve Telegram mümkün"),
    
    // Database errors (DB_XXX)
    DB_001("DB_001", "Veritabanı bağlantı hatası"),
    DB_002("DB_002", "Eşzamanlı güncelleme hatası"),
    DB_003("DB_003", "Veri bütünlüğü hatası"),
    
    // Auth errors (AUTH_XXX)
    AUTH_001("AUTH_001", "Kullanıcı bulunamadı"),
    AUTH_002("AUTH_002", "Şifre hatalı"),
    AUTH_003("AUTH_003", "Email zaten kayıtlı"),
    
    // System errors (SYS_XXX)
    SYS_001("SYS_001", "Bilinmeyen sistem hatası"),
    SYS_002("SYS_002", "Işlem sırasında hata oluştu"),

    // Application errors (APP_XXX)
    APP_001("APP_001", "Bu turnuvaya zaten başvurdunuz"),
    APP_002("APP_002", "Takım turnuvalarında clan seçimi zorunludur"),
    APP_003("APP_003", "Clan'ın kategorisi turnuva kategorisi ile uyuşmuyor"),
    APP_004("APP_004", "Sadece clan sahibi veya takım kaptanı turnuvaya başvurabilir"),
    APP_005("APP_005", "Turnuva başvurusu işlemi sırasında hata oluştu"),
    APP_006("APP_006", "Başvuru bulunamadı");

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
