# Profile Sistemi - Kurulum Özeti

## ✅ Oluşturulan Dosyalar

### 1. Entity'ler
- **UserProfileSettings.java** - Profil ayarları ve gizlilik seçenekleri
  - userId (unique)
  - showProfile, showClans, showRatings, showBio
  - allowDirectMessages
  - bio (500 karakter)
  - isPrivate

### 2. Repository'ler
- **UserProfileSettingsRepository.java** - Settings yönetimi
- **ClanMemberRepository.java** (güncellendi) - findUserActiveClans() method eklendi

### 3. DTO'lar
- **UserProfileResponseDTO.java** - Profil yanıtı (cüzdan bilgisi YOK)
  - UserProfileResponseDTO.ClanProfileDTO - Klan bilgileri
  - UserProfileResponseDTO.UserRatingDTO - Kritikler
  - UserProfileResponseDTO.UserProfileSettingsDTO - Ayarlar (sadece kendi profili)

### 4. Request'ler
- **UpdateProfileSettingsRequest.java** - Profil ayarları güncelleme

### 5. Service'ler
- **UserService.java** (güncellendi)
  - getOrCreateProfileSettings() - Settings oluştur/getir
  - getUserProfile(userId) - Profil görüntüle (gizlilik kontrollü)
  - updateProfileSettings() - Ayarlar güncelle

### 6. Controller'ler
- **UserController.java** (güncellendi)
  - GET /api/users/{userId}/profile - Profil görüntüle
  - PUT /api/users/profile/settings - Ayarlar güncelle

## 📋 Endpoint'ler

### 1. Profil Görüntüleme
```
GET /api/users/{userId}/profile
```
**Yanıt:**
```json
{
  "id": 1,
  "display_name": "Oyuncu1",
  "tag": "@oyuncu1",
  "bio": "Oyun severim",
  "isPrivate": false,
  "clans": [
    {
      "clanId": 5,
      "clanName": "Alpha Team",
      "clanRole": "OWNER",
      "categoryId": 1,
      "categoryName": "Valorant"
    }
  ],
  "ratings": [],
  "settings": null  // Sadece kendi profili görseydi burada olurdu
}
```

### 2. Profil Ayarlarını Güncelle
```
PUT /api/users/profile/settings
```
**İstek Gövdesi:**
```json
{
  "bio": "Yeni bio metni",
  "showProfile": true,
  "showClans": true,
  "showRatings": true,
  "showBio": true,
  "allowDirectMessages": true,
  "isPrivate": false
}
```

## 🔒 Güvenlik Özellikleri

1. **Cüzdan Bilgisi Gizli** - Profil response'de cüzdan bilgisi hiç gösterilmez
2. **Gizlilik Kontrolleri** - isPrivate = true ise profil erişilebilir değil
3. **XSS Koruması** - Bio metni sanitize edilir (500 karakter)
4. **Kimlik Doğrulama** - Ayarları sadece kendi profili görebilir

## 🎯 Özellikler

✅ Kullanıcılar kendi profini görebilir
✅ Kullanıcılar başka profilini görebilir (gizlilik hariç)
✅ Cüzdan değerleri asla gösterilmez
✅ DB'de settings kısmı var (hangi bilgiler gösterilecek)
✅ Bio (açıklama) destekleniyor
✅ Kendini görünmez yapabilir (isPrivate = true)
✅ Hangi klanlarda olduğunu görebilir
✅ Kritikler için hazırlanmış (TODO: Rating entity gerekli)

## 📝 TODO'lar

1. Rating Entity oluştur ve integration yap
2. /api/users/my/profile endpoint'i tamamla (getCurrentUserId ile)
3. Profil güncelleme endpoint'i ekle (display_name, tag vs.)
4. Başkalarına mesaj gönderme izni kontrolleri (allowDirectMessages)


