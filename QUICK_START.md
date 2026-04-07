# 🚀 Hızlı Başlangıç Rehberi

## 📍 Test Çalıştırma

### 1. organizationId=1 için ilk turnuva
```bash
POST http://localhost:8080/api/turnuva/create
Content-Type: application/json

{
  "organizationId": 1,
  "category": "Yazılım",
  "title": "Turnuva 1",
  "description": "İlk turnuvamız",
  "start_date": "2024-04-15T10:00:00",
  "finish_date": "2024-04-20T18:00:00",
  "isActive": true
}
```

**Başarılı Yanıt (201 Created):**
```json
{
  "status": true,
  "message": "Turnuva başarıyla oluşturuldu. ID: 1"
}
```

---

### 2. Aynı organizationId=1 için IKINCI turnuva
```bash
POST http://localhost:8080/api/turnuva/create
Content-Type: application/json

{
  "organizationId": 1,
  "category": "Yazılım",
  "title": "Turnuva 2",
  "description": "İkinci turnuvamız",
  "start_date": "2024-04-25T10:00:00",
  "finish_date": "2024-04-30T18:00:00",
  "isActive": true
}
```

**Başarılı Yanıt (201 Created):**
```json
{
  "status": true,
  "message": "Turnuva başarıyla oluşturuldu. ID: 2"
}
```

✅ **HER İKİ TURNUVA DA AYRILI ŞEKILDE KAYITLANDI!**

---

### 3. Error Code Örneği - Tarih Hatası
```bash
POST http://localhost:8080/api/turnuva/create
Content-Type: application/json

{
  "organizationId": 1,
  "category": "Yazılım",
  "title": "Hatalı Turnuva",
  "description": "Test",
  "start_date": "2024-04-20T18:00:00",
  "finish_date": "2024-04-15T10:00:00"
}
```

**Hata Yanıt (400 Bad Request):**
```json
{
  "status": 400,
  "errorCode": "VAL_003",
  "message": "Başlangıç tarihi bitiş tarihinden sonra olamaz",
  "error": "Turnuva Hatası",
  "details": "start_date: Sat Apr 20 2024 ... > finish_date: Sun Apr 15 2024 ...",
  "timestamp": 1712502000000
}
```

💡 **Kolay tespit:** `errorCode: "VAL_003"` sayesinde sorun hemen bulunuyor!

---

### 4. Boş Alan Hatası
```bash
POST http://localhost:8080/api/turnuva/create
Content-Type: application/json

{
  "organizationId": 1,
  "category": "",
  "title": "",
  "description": "Test",
  "start_date": "2024-04-15T10:00:00",
  "finish_date": "2024-04-20T18:00:00"
}
```

**Hata Yanıt (400 Bad Request):**
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

### 5. Tüm Turnuvaları Listele
```bash
GET http://localhost:8080/api/turnuva/list
```

**Yanıt:**
```json
[
  {
    "id": 1,
    "organizationId": 1,
    "category": "Yazılım",
    "title": "Turnuva 1",
    "description": "İlk turnuvamız",
    "start_date": "2024-04-15T10:00:00",
    "finish_date": "2024-04-20T18:00:00",
    "isActive": true,
    "imageUrl": null,
    "link": null,
    "link_type": null
  },
  {
    "id": 2,
    "organizationId": 1,
    "category": "Yazılım",
    "title": "Turnuva 2",
    "description": "İkinci turnuvamız",
    "start_date": "2024-04-25T10:00:00",
    "finish_date": "2024-04-30T18:00:00",
    "isActive": true,
    "imageUrl": null,
    "link": null,
    "link_type": null
  }
]
```

---

### 6. Belirli Organizasyonun Turnuvaları
```bash
GET http://localhost:8080/api/turnuva/organization/1
```

**Yanıt:** Yukarıdaki gibi (organizationId=1 olan tüm turnuvalar)

---

## 📊 Error Code Referans

| Code | Anlamı | HTTP Status |
|------|--------|------------|
| VAL_001 | Zorunlu alan boş | 400 |
| VAL_002 | Tarih formatı hatalı | 400 |
| VAL_003 | Başlangıç > Bitiş tarihi | 400 |
| TRN_001 | Turnuva bulunamadı | 400 |
| TRN_005 | Listeleme hatası | 500 |
| DB_002 | Eşzamanlı işlem | 500 |
| SYS_001 | Bilinmeyen hata | 500 |

---

## 🔧 Swagger'da Test

1. Swagger'ı açın: `http://localhost:8080/swagger-ui.html`
2. "Turnuva" kısmını genişletin
3. Endpoint'i seçin
4. "Try it out" tıklayın
5. Request body girin
6. "Execute" tıklayın

---

## 📝 Sık Sorulan Sorular

**S: organizationId 1 için ikinci turnuva açılamaz mı?**
- C: Hayır! Açılır. Her turnuva ayrı kaydedilir. Güncelleme yapılmaz.

**S: Hata mesajında stacktrace var mı?**
- C: Hayır! Sadece user-friendly mesaj. Teknik detaylar logger'a yazılır.

**S: Error code'lar nereyi bulabilirim?**
- C: Hata yanıtında `errorCode` alanında. Örn: `"errorCode": "VAL_003"`

**S: Concurrent request geldiğinde ne olur?**
- C: Artık sorun yok! Her request yeni bir insert yapar.

---

## 🚀 Üretim Deployment

```bash
# Paketi oluştur
mvn clean package

# Çalıştır
java -jar target/meydan-0.0.1-SNAPSHOT.jar
```

**Port:** 8080
**Swagger:** http://localhost:8080/swagger-ui.html
**API Dokümantasyon:** http://localhost:8080/v3/api-docs

---

## 🎯 Sonuç

✅ **Sorun 1:** organizationId 1 için ikinci turnuva - ÇÖZÜLDÜş Unlimited destek!
✅ **Sorun 2:** Hata mesajı stacktrace - ÇÖZÜLDÜ! User-friendly mesajlar
✅ **Sorun 3:** Error code sistemi - ÇÖZÜLDÜ! Kolay bulunabilir

Artık production'a hazır! 🎉

