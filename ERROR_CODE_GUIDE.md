# Error Code Sistemi - Kullanım Kılavuzu

## 📋 Error Code Kategorileri

### 1. **Doğrulama Hataları (VAL_XXX)**
- `VAL_001` - Zorunlu alan boş bırakılamaz
- `VAL_002` - Tarih formatı hatalı
- `VAL_003` - Başlangıç tarihi bitiş tarihinden sonra olamaz

### 2. **Turnuva Hataları (TRN_XXX)**
- `TRN_001` - Turnuva bulunamadı
- `TRN_002` - Turnuva oluşturulurken hata oluştu
- `TRN_003` - Turnuva güncellenirken hata oluştu
- `TRN_004` - Turnuva silinirken hata oluştu
- `TRN_005` - Turnuva listelenirken hata oluştu

### 3. **Veritabanı Hataları (DB_XXX)**
- `DB_001` - Veritabanı bağlantı hatası
- `DB_002` - Eşzamanlı güncelleme hatası ⚠️ (Concurrent request)
- `DB_003` - Veri bütünlüğü hatası

### 4. **Kimlik Doğrulama Hataları (AUTH_XXX)**
- `AUTH_001` - Kullanıcı bulunamadı
- `AUTH_002` - Şifre hatalı
- `AUTH_003` - Email zaten kayıtlı

### 5. **Sistem Hataları (SYS_XXX)**
- `SYS_001` - Bilinmeyen sistem hatası
- `SYS_002` - Işlem sırasında hata oluştu

---

## 📱 Hata Yanıt Formatı

```json
{
  "status": 400,
  "errorCode": "VAL_003",
  "message": "Başlangıç tarihi bitiş tarihinden sonra olamaz",
  "error": "Doğrulama Hatası",
  "details": "start_date: 2024-04-20T18:00:00 > finish_date: 2024-04-15T10:00:00",
  "timestamp": 1712502000000
}
```

### Alan Açıklamaları:
- `status` - HTTP Status Code
- `errorCode` - Hata kodu (kolay bulunabilirlik için)
- `message` - Kullanıcı dostu hata mesajı
- `error` - Hata kategorisi
- `details` - Teknik detaylar (Optional - hata türüne göre)
- `timestamp` - İşlem zamanı

---

## 🧪 Test Senaryoları

### 1. Unlimited Turnuva Oluşturma
organizationId 1 için birden fazla turnuva açabilirsiniz:

**İstek 1:**
```json
{
  "organizationId": "1",
  "category": "Yazılım",
  "title": "Turnuva 1",
  "description": "İlk turnuva",
  "start_date": "2024-04-15T10:00:00",
  "finish_date": "2024-04-20T18:00:00",
  "isActive": true
}
```

**İstek 2 (Aynı organizationId ile):**
```json
{
  "organizationId": "1",
  "category": "Yazılım",
  "title": "Turnuva 2",
  "description": "İkinci turnuva",
  "start_date": "2024-04-25T10:00:00",
  "finish_date": "2024-04-30T18:00:00",
  "isActive": true
}
```

✅ Her iki turnuva da oluşturulacak. Güncelleme yapılmayacak!

---

### 2. Tarih Hatası
```json
{
  "organizationId": "1",
  "category": "Yazılım",
  "title": "Test",
  "description": "Test",
  "start_date": "2024-04-20T18:00:00",
  "finish_date": "2024-04-15T10:00:00",
  "isActive": true
}
```

**Yanıt:**
```json
{
  "status": 400,
  "errorCode": "VAL_003",
  "message": "Başlangıç tarihi bitiş tarihinden sonra olamaz",
  "error": "Turnuva Hatası",
  "details": "start_date: ... > finish_date: ...",
  "timestamp": 1712502000000
}
```

---

### 3. Boş Alan Hatası
```json
{
  "organizationId": "1",
  "category": "",
  "title": "",
  "description": "Test",
  "start_date": "2024-04-15T10:00:00",
  "finish_date": "2024-04-20T18:00:00"
}
```

**Yanıt:**
```json
{
  "status": 400,
  "errorCode": "VAL_001",
  "message": "category: Kategori boş bırakılamaz, title: Başlık boş bırakılamaz",
  "error": "Doğrulama Hatası",
  "details": null,
  "timestamp": 1712502000000
}
```

---

### 4. Eşzamanlı Güncelleme Hatası (ÖNCEKİ SORUN)
Artık çözüldü! Hiç güncelleme yok, her turnuva yeni kayıt olarak açılıyor.

**Eski Hata:**
```json
{
  "status": 500,
  "errorCode": "DB_002",
  "message": "Aynı anda birden fazla işlem yapılamaz. Lütfen biraz bekleyip tekrar deneyiniz.",
  "error": "Sunucu Hatası",
  "details": null,
  "timestamp": 1712502000000
}
```

---

## 🔍 Logger'da Hatalar

Tüm hatalar ayrıntılı şekilde logger'a yazılıyor:
```
ERROR - Turnuva oluşturma hatası: Başlangıç tarihi bitiş tarihinden sonra olamaz
ERROR - Details: start_date: ... > finish_date: ...
```

Hata detayları sadece **Logger**'da görülüyor, API yanıtında gösterilmiyor ✅

---

## 📍 Yeni Endpoint

### OrganizationId'ye göre Turnuvaları Listele
```
GET /api/turnuva/organization/{organizationId}
```

**Örnek:**
```
GET /api/turnuva/organization/1
```

**Yanıt:**
```json
[
  {
    "id": 1,
    "organizationId": 1,
    "category": "Yazılım",
    "title": "Turnuva 1",
    "description": "İlk turnuva",
    "isActive": true
  },
  {
    "id": 2,
    "organizationId": 1,
    "category": "Yazılım",
    "title": "Turnuva 2",
    "description": "İkinci turnuva",
    "isActive": true
  }
]
```

---

## ✅ Özet Değişiklikleri

- [x] **ErrorCode Enum** - Tüm hata kodları merkezileştirildi
- [x] **ErrorResponse** - errorCode ve details alanları eklendi
- [x] **GlobalExceptionHandler** - Tüm exception'lar logger'a yazılıyor
- [x] **Unlimited Turnuva** - Her turnuva yeni kayıt olarak açılıyor
- [x] **Hata Mesajları** - Stacktrace yok, sadece user-friendly mesajlar
- [x] **TurnuvaService** - Logger desteği ve detaylı error handling
- [x] **TurnuvaRepository** - organizationId sorgusu eklendi
- [x] **TurnuvaController** - Yeni endpoint: /organization/{organizationId}

