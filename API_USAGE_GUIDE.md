# 📚 Category & Turnuva API - Detaylı Kullanım Rehberi

## 🎯 Overview

Bu API, profesyonel seviye CRUD (Create, Read, Update, Delete) işlemleri sunar:
- ✅ Soft Delete & Restore (veri koruması)
- ✅ Pagination desteği (büyük veri setleri için)
- ✅ IDOR koruması (JWT tabanlı)
- ✅ XSS & Link Validasyonu
- ✅ Kapsamlı error handling

---

## 🔑 Authentication

Tüm endpoint'ler (public olmayan) için JWT token gereklidir:

```
Header: Authorization: Bearer <JWT_TOKEN>
```

---

## 📋 CATEGORY API

### 1️⃣ **Ana Kategorileri Listele**

**Endpoint**: `GET /api/category/list`

**Description**: Aktif ana kategorileri getirir (parent null olanlar)

**Response**:
```json
{
  "success": true,
  "message": "Ana kategoriler başarıyla getirildi",
  "data": [
    {
      "id": 1,
      "name": "Games",
      "slug": "games",
      "image": "https://example.com/games.jpg",
      "description": "Game categories",
      "parent": null,
      "children": [
        {
          "id": 2,
          "name": "PUBG Mobile",
          "slug": "pubg-mobile",
          "parent": null,
          "children": []
        }
      ]
    }
  ]
}
```

### 2️⃣ **Alt Kategorileri Listele**

**Endpoint**: `GET /api/category/{parentId}/subcategories`

**Example**: `GET /api/category/1/subcategories`

**Description**: Belirli bir ana kategorinin alt kategorilerini getirir

**Response**:
```json
{
  "success": true,
  "message": "Alt kategoriler başarıyla getirildi",
  "data": [
    {
      "id": 2,
      "name": "PUBG Mobile",
      "slug": "pubg-mobile",
      "parent": {
        "id": 1,
        "name": "Games"
      }
    }
  ]
}
```

### 3️⃣ **Yeni Kategori Oluştur**

**Endpoint**: `POST /api/category/create`

**Request Body** (Ana Kategori):
```json
{
  "name": "Games",
  "image": "https://example.com/games.jpg",
  "description": "Game categories",
  "parentId": null
}
```

**Request Body** (Alt Kategori):
```json
{
  "name": "PUBG Mobile",
  "image": "https://example.com/pubg.jpg",
  "description": "PUBG Mobile tournaments",
  "parentId": 1
}
```

**Response**:
```json
{
  "success": true,
  "message": "Kategori başarıyla oluşturuldu",
  "data": {
    "id": 2,
    "name": "PUBG Mobile",
    "slug": "pubg-mobile",
    "parent": {
      "id": 1,
      "name": "Games"
    }
  }
}
```

### 4️⃣ **Kategori Güncelle**

**Endpoint**: `PUT /api/category/update`

**Request Body**:
```json
{
  "id": 2,
  "name": "PUBG Mobile Updated",
  "image": "https://example.com/pubg-updated.jpg",
  "description": "Updated description",
  "parentId": 1
}
```

**Response**:
```json
{
  "success": true,
  "message": "Kategori başarıyla güncellendi",
  "data": {
    "id": 2,
    "name": "PUBG Mobile Updated",
    "slug": "pubg-mobile-updated"
  }
}
```

---

## 🎮 TURNUVA API

### 1️⃣ **Turnuva Oluşturma**

**Endpoint**: `POST /api/turnuva/create`

**Headers**: `Authorization: Bearer <JWT_TOKEN>`

**Request Body**:
```json
{
  "categoryId": 1,
  "title": "PUBG Mobile Arena Championship",
  "description": "Türkiye'nin en büyük esports turnuvası",
  "start_date": "2026-04-15T09:00:00",
  "finish_date": "2026-04-20T18:00:00",
  "link": "https://www.instagram.com/meydan_esports",
  "imageUrl": "https://example.com/tournament.jpg",
  "isActive": true,
  "reward_amount": 5000.00,
  "reward_currency": "game_currency",
  "player_format": "5v5"
}
```

**Validation Rules**:
- `categoryId`: @NotNull, zorunlu ve var olmalı
- `title`: @NotBlank, zorunlu
- `description`: @NotBlank, zorunlu
- `start_date`: @NotNull, zorunlu
- `finish_date`: @NotNull, zorunlu
- **start_date < finish_date** (tarih kontrolü)
- `link`: Opsiyonel ama belirtilirse sosyal ağ URL'si olmalı
- `reward_amount`: Opsiyonel, Double
- `reward_currency`: Opsiyonel, String (game_currency, tl, usd, etc.)
- `player_format`: Opsiyonel, String (5v5, 1v1, etc.)

**Link Validasyonu**:
- Desteklenen sosyal ağlar: Instagram, WhatsApp, Discord, Telegram
- Örnek URL'ler:
  - ✅ `https://www.instagram.com/username`
  - ✅ `https://wa.me/905xxxxxxxxx`
  - ✅ `https://discord.gg/xyz`
  - ✅ `https://t.me/username`
  - ❌ `https://facebook.com/...` (desteklenmez)

**Success Response (201)**:
```json
{
  "success": true,
  "message": "Turnuva başarıyla oluşturuldu",
  "data": {
    "id": 1,
    "oid": "550e8400-e29b-41d4-a716-446655440001",
    "organizationId": 5,
    "category": {
      "id": 1,
      "name": "Arena"
    },
    "title": "PUBG Mobile Arena Championship",
    "description": "Türkiye'nin en büyük esports turnuvası",
    "start_date": "2026-04-15T09:00:00",
    "finish_date": "2026-04-20T18:00:00",
    "link": "https://www.instagram.com/meydan_esports",
    "link_type": "INSTAGRAM",
    "imageUrl": "https://example.com/tournament.jpg",
    "isActive": true
  }
}
```

**Error Response (400) - Tarih Hatası**:
```json
{
  "success": false,
  "message": "Başlangıç tarihi bitiş tarihinden sonra olamaz",
  "data": null
}
```

**Error Response (400) - Link Hatası**:
```json
{
  "success": false,
  "message": "Geçersiz sosyal ağ URL'si - Sadece Instagram, WhatsApp, Discord veya Telegram URL'leri desteklenir",
  "data": null
}
```

**Error Response (404) - Kategori Hatası**:
```json
{
  "success": false,
  "message": "Geçersiz kategori ID",
  "data": null
}
```

---

### 2️⃣ **Turnuva Listeleme**

#### A) Tüm Turnuvaları Listele
**Endpoint**: `GET /api/turnuva/list`

#### B) Turnuvaları Sayfalı Listele
**Endpoint**: `GET /api/turnuva/list/paginated?page=0&size=10`

#### C) Belirli Organizasyonun Turnuvalarını Listele
**Endpoint**: `GET /api/turnuva/{organizationId}`

#### D) Belirli Organizasyonun Turnuvalarını Sayfalı Listele
**Endpoint**: `GET /api/turnuva/{organizationId}/paginated?page=0&size=10`

#### E) Kendi Turnuvalarımı Listele (JWT Tabanlı)
**Endpoint**: `GET /api/turnuva/my`

**Headers**: `Authorization: Bearer <JWT_TOKEN>`

**Notes**:
- JWT token'dan alınan user ID kullanılır
- Sadece kendi organizasyonunun turnuvaları döner

#### F) Kendi Turnuvalarımı Sayfalı Listele
**Endpoint**: `GET /api/turnuva/my/paginated?page=0&size=10`

**Headers**: `Authorization: Bearer <JWT_TOKEN>`

---

### 3️⃣ **Turnuva Güncelleme**

**Endpoint**: `PUT /api/turnuva/update`

**Headers**: `Authorization: Bearer <JWT_TOKEN>`

**Request Body**:
```json
{
  "id": 1,
  "categoryId": 2,
  "title": "PUBG Mobile Arena Championship (Güncellenmiş)",
  "description": "Güncellenmiş açıklama",
  "start_date": "2026-04-15T10:00:00",
  "finish_date": "2026-04-21T18:00:00",
  "link": "https://www.discord.com/invite/xyz",
  "imageUrl": "https://example.com/tournament-new.jpg",
  "isActive": true,
  "reward_amount": 7500.00,
  "reward_currency": "tl",
  "player_format": "3v3"
}
```

**Security**:
- **IDOR Koruması**: JWT token'dan alınan organizationId ile turnuva sahibi doğrulanır
- Sadece kendi turnuvalarınızı güncelleyebilirsiniz

**Error Response (403) - IDOR**:
```json
{
  "success": false,
  "message": "Bu turnuvayı güncellemek için yetkiniz yok",
  "data": null
}
```

**Error Response (400) - Silinmiş Turnuva**:
```json
{
  "success": false,
  "message": "Silinmiş turnuva güncellenemez",
  "data": null
}
```

---

### 4️⃣ **Turnuva Silme (Soft Delete)**

**Endpoint**: `DELETE /api/turnuva/{id}`

**Example**: `DELETE /api/turnuva/1`

**Headers**: `Authorization: Bearer <JWT_TOKEN>`

**Security**: IDOR korumalı

**Success Response (200)**:
```json
{
  "success": true,
  "message": "Turnuva başarıyla silindi",
  "data": {
    "id": 1,
    "isActive": false,
    ...
  }
}
```

---

### 5️⃣ **Turnuva Geri Yükleme**

**Endpoint**: `POST /api/turnuva/{id}/restore`

**Example**: `POST /api/turnuva/1/restore`

**Headers**: `Authorization: Bearer <JWT_TOKEN>`

**Security**: IDOR korumalı

**Success Response (200)**:
```json
{
  "success": true,
  "message": "Turnuva başarıyla geri yüklendi",
  "data": {
    "id": 1,
    "isActive": true,
    ...
  }
}
```

---

### 6️⃣ **Turnuva Kalıcı Silme**

**Endpoint**: `DELETE /api/turnuva/{id}/permanent`

**Example**: `DELETE /api/turnuva/1/permanent`

**Headers**: `Authorization: Bearer <JWT_TOKEN>`

**Security**: IDOR korumalı

**Warning**: Geri alınamaz!

---

## 🔒 Güvenlik Özellikleri Detayı

### IDOR (Insecure Direct Object Reference) Koruması

Turnuva işlemleri için uygulanır:
```
JWT Token → Decode → User ID (organizationId olarak kullanılır)
                ↓
        Turnuva.organizationId kontrolü
                ↓
        Eşit mi? → Operasyon izin ver
        Eşit değil mi? → 403 Forbidden + Log kaydı
```

**Log Örneği**:
```
WARN: IDOR saldırısı tespit edildi. 
      OrganizationId: 5, 
      Turnuva OrganizationId: 10
```

### XSS (Cross-Site Scripting) Koruması

Turnuva title ve description alanlarında:
```
Input: "<script>alert('xss')</script>"
          ↓
Process: Sanitize & Limit
          ↓
Output: "alertxss" (scripts kaldırılmış)
          ↓
Log: "XSS saldırısı tespit edildi ve temizlendi"
```

### Link Validasyonu

Dinamik sosyal ağ türü belirleme:
```
Input: "https://www.instagram.com/meydan"
          ↓
Validate: isValidSocialMediaUrl()
          ↓
Detect: detectSocialMediaType() → INSTAGRAM
          ↓
Set: link_type = "INSTAGRAM"
```

---

## 📊 HTTP Status Codes

| Code | Durum | Açıklama |
|------|-------|----------|
| 200 | OK | Başarılı GET, PUT, POST (restore), DELETE |
| 201 | Created | Başarılı POST (create) |
| 400 | Bad Request | Validasyon hatası, tarih hatası, zaten silinmiş |
| 401 | Unauthorized | JWT token eksik veya geçersiz |
| 403 | Forbidden | IDOR - Yetkisiz erişim |
| 404 | Not Found | Kaynak bulunamadı |
| 500 | Internal Server Error | Server hatası |

---

## 🧪 Örnek Test Senaryoları

### Test 1: Pagination
```bash
curl -X GET "http://localhost:8080/api/category/list/paginated?page=0&size=5" \
  -H "Authorization: Bearer TOKEN"
```

### Test 2: IDOR Koruması
```bash
# User A (organizationId=1)
curl -X PUT "http://localhost:8080/api/turnuva/update" \
  -H "Authorization: Bearer USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id": 99, "categoryId": 1, ...}' # User B'nin turnuvaları

# Result: 403 Forbidden
```

### Test 3: Soft Delete ve Restore
```bash
# Sil
curl -X DELETE "http://localhost:8080/api/turnuva/1" \
  -H "Authorization: Bearer TOKEN"

# Geri Yükle
curl -X POST "http://localhost:8080/api/turnuva/1/restore" \
  -H "Authorization: Bearer TOKEN"
```

### Test 4: XSS Koruma
```bash
curl -X POST "http://localhost:8080/api/turnuva/create" \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 1,
    "title": "<script>alert(\"xss\")</script>",
    "description": "Test",
    "start_date": "2026-04-15T00:00:00",
    "finish_date": "2026-04-20T00:00:00"
  }'

# Result: Script tagları kaldırılır, log kaydı yapılır
```

---

## 📝 Dosya Harita

```
src/main/java/com/meydan/meydan/
├── controller/
│   ├── CategoryController.java (✅ 7 endpoint)
│   └── TurnuvaController.java (✅ 10 endpoint)
├── service/
│   ├── CategoryService.java (✅ 10 metod)
│   └── TurnuvaService.java (✅ 11 metod)
├── repository/
│   ├── CategoryRepository.java (✅ 4 query method)
│   └── TurnuvaRepository.java (✅ 2 query method)
└── request/Auth/
    ├── Category/
    │   ├── AddCategoryRequestBody.java
    │   ├── UpdateCategoryRequestBody.java
    │   └── DeleteCategoryRequestBody.java
    └── Turnuva/
        ├── AddTurnuvaRequestBody.java
        └── UpdateTurnuvaRequestBody.java
```

---

**✅ Build Status**: SUCCESS
**✅ Compilation**: 36 source files
**✅ Total Endpoints**: 17 (Category: 7, Turnuva: 10)
**✅ Security**: IDOR + XSS + Link Validation
**✅ Pagination**: Spring Data JPA ile full support
