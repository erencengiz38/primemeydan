# Profile API - Test Örnekleri

## 1. Kendi Profilimi Görüntüle
```bash
GET /api/users/me/profile
Authorization: Bearer <token>
```

**Beklenen Yanıt (200 OK):**
```json
{
  "success": true,
  "message": "Kendi profiliniz getirildi.",
  "data": {
    "id": 1,
    "oid": "550e8400-e29b-41d4-a716-446655440000",
    "display_name": "Oyuncu1",
    "tag": "@oyuncu1",
    "bio": "CS:GO ve Valorant oynuyorum",
    "isPrivate": false,
    "clans": [
      {
        "clanId": 5,
        "clanName": "Alpha Team",
        "clanRole": "OWNER",
        "categoryId": 1,
        "categoryName": "Valorant"
      },
      {
        "clanId": 10,
        "clanName": "Beta Squad",
        "clanRole": "MEMBER",
        "categoryId": 2,
        "categoryName": "CS:GO"
      }
    ],
    "ratings": [],
    "settings": {
      "showProfile": true,
      "showClans": true,
      "showRatings": true,
      "showBio": true,
      "allowDirectMessages": true,
      "isPrivate": false
    }
  }
}
```

---

## 2. Başka Bir Kullanıcının Profilini Görüntüle
```bash
GET /api/users/2/profile
Authorization: Bearer <token>
```

**Beklenen Yanıt (200 OK) - settings yok:**
```json
{
  "success": true,
  "message": "Profil başarıyla getirildi.",
  "data": {
    "id": 2,
    "oid": "660e8400-e29b-41d4-a716-446655440001",
    "display_name": "Oyuncu2",
    "tag": "@oyuncu2",
    "bio": "Valorant takım oyuncu",
    "isPrivate": false,
    "clans": [
      {
        "clanId": 5,
        "clanName": "Alpha Team",
        "clanRole": "MANAGER",
        "categoryId": 1,
        "categoryName": "Valorant"
      }
    ],
    "ratings": [],
    "settings": null
  }
}
```

---

## 3. Gizli Profile Erişim Denemesi
```bash
GET /api/users/3/profile
Authorization: Bearer <token>
```

**Beklenen Yanıt (403 Forbidden):**
```json
{
  "success": false,
  "message": "Bu profil gizlidir. Erişim yetkiniz yok.",
  "data": null
}
```

---

## 4. Profil Ayarlarını Güncelle
```bash
PUT /api/users/profile/settings
Authorization: Bearer <token>
Content-Type: application/json

{
  "bio": "Yeni bio yazı",
  "showProfile": true,
  "showClans": true,
  "showRatings": false,
  "showBio": true,
  "allowDirectMessages": true,
  "isPrivate": false
}
```

**Beklenen Yanıt (200 OK):**
```json
{
  "success": true,
  "message": "Profil ayarları başarıyla güncellendi.",
  "data": {
    "showProfile": true,
    "showClans": true,
    "showRatings": false,
    "showBio": true,
    "allowDirectMessages": true,
    "isPrivate": false
  }
}
```

---

## 5. Profil Bilgilerini Güncelle (Display Name + Bio)
```bash
PUT /api/users/me/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "display_name": "Yeni Adım",
  "bio": "Yeni bio yazı"
}
```

**Beklenen Yanıt (200 OK):**
```json
{
  "success": true,
  "message": "Profil başarıyla güncellendi.",
  "data": {
    "id": 1,
    "oid": "550e8400-e29b-41d4-a716-446655440000",
    "display_name": "Yeni Adım",
    "tag": "@oyuncu1",
    "bio": "Yeni bio yazı",
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
    "settings": {
      "showProfile": true,
      "showClans": true,
      "showRatings": true,
      "showBio": true,
      "allowDirectMessages": true,
      "isPrivate": false
    }
  }
}
```

---

## 6. Kendini Gizli Yap (isPrivate = true)
```bash
PUT /api/users/profile/settings
Authorization: Bearer <token>
Content-Type: application/json

{
  "isPrivate": true
}
```

**Beklenen Yanıt (200 OK):**
```json
{
  "success": true,
  "message": "Profil ayarları başarıyla güncellendi.",
  "data": {
    "showProfile": true,
    "showClans": true,
    "showRatings": true,
    "showBio": true,
    "allowDirectMessages": true,
    "isPrivate": true
  }
}
```

Bundan sonra başkaları bu profili görüntülemek istediğinde 403 hatası alacak.

---

## 7. Klanlık Bilgisini Gizle
```bash
PUT /api/users/profile/settings
Authorization: Bearer <token>
Content-Type: application/json

{
  "showClans": false
}
```

**Beklenen Yanıt:** Profil sorgulandığında `clans` list'i boş olacak.

---

## Notlar

- ✅ Cüzdan bilgisi (`meydanCoin`, `realBalance`) hiçbir zaman response'de gösterilmez
- ✅ `settings` alanı sadece kendi profili görüntülendiğinde gösterilir
- ✅ Bio metni XSS korumasıyla sanitize edilir (max 500 karakter)
- ✅ Display name sanitize edilir (max 100 karakter)
- ✅ Kritikler (`ratings`) şimdilik boş liste dönüyor (Rating entity'si gerekli)
- ✅ `isPrivate=true` olduğunda diğer kullanıcılar profili göremez
- ✅ Her görünürlük seçeneği ayrıca kontrol edilir (`showClans`, `showRatings`, `showBio`)

