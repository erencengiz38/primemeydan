# Meydan API - Güncellemeler

## 🎯 Yapılan Değişiklikler

### 1. **Turnuva Oluşturma - Unlimited Destek**
- ✅ Kullanıcılar artık **istenildiği kadar turnuva** oluşturabilirler
- Herhangi bir 1-1 sınırlaması kaldırıldı
- Tarih validasyonu eklendi (başlangıç tarihi bitiş tarihinden sonra olamaz)

### 2. **Hata Yönetimi - Global Exception Handler**
Artık tüm hatalar düzgün, okunabilir formatta döndürülüyor:

```json
{
  "status": 400,
  "message": "Başlık boş bırakılamaz",
  "error": "Doğrulama Hatası",
  "timestamp": 1712502000000
}
```

### 3. **Request Validation**
TurnuvaRequest'e zorunlu alan validasyonları eklendi:
- `organizationId` - Zorunlu
- `category` - Zorunlu, boş olamaz
- `title` - Zorunlu, boş olamaz
- `description` - Zorunlu, boş olamaz
- `start_date` - Zorunlu
- `finish_date` - Zorunlu

### 4. **Swagger Documentation**
- ✅ Swagger endpoint'leri security'de açıldı
- ✅ Swagger UI erişilebilir
- API dokümantasyonu otomatik oluşturuluyor

## 📍 API Endpoints

### Turnuva Oluştur
```
POST /api/turnuva/create
Content-Type: application/json

{
  "organizationId": "1",
  "category": "Yazılım",
  "title": "Web Dev Turnuvası",
  "description": "Açık web geliştirme turnuvası",
  "start_date": "2024-04-15T10:00:00",
  "finish_date": "2024-04-20T18:00:00",
  "isActive": true,
  "link": "https://example.com",
  "link_type": "URL"
}
```

### Turnuvaları Listele
```
GET /api/turnuva/list
```

## 🛡️ Hata Örnekleri

### Validasyon Hatası (400)
```json
{
  "status": 400,
  "message": "başlık: Başlık boş bırakılamaz, açıklama: Açıklama boş bırakılamaz",
  "error": "Doğrulama Hatası",
  "timestamp": 1712502000000
}
```

### Tarih Hatası (400)
```json
{
  "status": 400,
  "message": "Başlangıç tarihi bitiş tarihinden sonra olamaz",
  "error": "Bad Request",
  "timestamp": 1712502000000
}
```

### Sunucu Hatası (500)
```json
{
  "status": 500,
  "message": "Bir hata oluştu: Database connection failed",
  "error": "Çalışma Zamanı Hatası",
  "timestamp": 1712502000000
}
```

## 📊 Oluşturulan/Güncellenen Dosyalar

### Yeni Dosyalar:
- `dto/ErrorResponse.java` - Hata yanıt modeli
- `exception/GlobalExceptionHandler.java` - Global exception handler
- `exception/TurnuvaException.java` - Custom exception sınıfı

### Güncellenmiş Dosyalar:
- `controller/TurnuvaController.java` - Validation ve HTTP status kodları
- `service/TurnuvaService.java` - Hata yönetimi
- `dto/TurnuvaRequest.java` - Validation anotasyonları

## 🚀 Test Etmek İçin

1. Uygulamayı başlatın
2. Swagger UI'a gidin: `http://localhost:8080/swagger-ui.html`
3. Turnuva API'sini test edin:
   - Geçerli veri ile turnuva oluşturun (201 Created)
   - Eksik veri gönderip hata yanıtlarını görin (400 Bad Request)
   - Tarih hataları test edin

## ✅ Tamamlanan Görevler

- [x] Unlimited turnuva oluşturma desteği
- [x] Global exception handling
- [x] Input validation
- [x] Anlaşılır hata mesajları
- [x] Swagger dokumentasyonu
- [x] HTTP status kodları

