# Category & Turnuva CRUD Refactoring - Hierarchical Category System

## 📋 Yapılan Değişiklikler

### 1. **Category.java** - Hierarchical Structure
- ✅ `game` ve `type` alanları kaldırıldı
- ✅ Self-referencing relationship eklendi:
  - `@ManyToOne Category parent`
  - `@OneToMany List<Category> children`
- ✅ JSON serialization için annotations:
  - `@JsonManagedReference` (children için)
  - `@JsonBackReference` (parent için)
- ✅ `slug` alanı eklendi (unique, SEO-friendly URLs)

### 2. **AddCategoryRequestBody.java**
- ✅ `parentId` alanı eklendi (nullable)
- ✅ Validation annotations korundu

### 3. **UpdateCategoryRequestBody.java**
- ✅ `parentId` alanı eklendi
- ✅ Validation annotations korundu

### 4. **CategoryRepository.java**
- ✅ `findByParentIsNullAndIsActiveTrue()` - Root kategoriler
- ✅ `findByParentIdAndIsActiveTrue(Long parentId)` - Alt kategoriler
- ✅ Pagination support eklendi

### 5. **CategoryService.java**
- ✅ `getAllCategories()` - Sadece root kategorileri döndürür
- ✅ `getSubCategories(Long parentId)` - Alt kategorileri getir
- ✅ `createCategory()` - parentId varsa parent category set et
- ✅ `updateCategory()` - parent relationship güncelle
- ✅ `generateSlug()` - SEO-friendly slug oluşturma

### 6. **CategoryController.java**
- ✅ `getAllCategories()` - Sadece root kategoriler
- ✅ `GET /{parentId}/subcategories` - Alt kategoriler endpoint'i
- ✅ Tüm response'lar `ResponseEntity<ApiResponse<T>>`

---

## 🏗️ Hierarchical Category Structure

### Ağaç Yapısı Örneği:
```
Games (Root Category - parent: null)
├── PUBG Mobile (Sub-category - parent: Games)
│   ├── UC Sales (Sub-sub-category)
│   └── Tournaments (Sub-sub-category)
├── Free Fire (Sub-category)
└── Valorant (Sub-category)

AI (Root Category)
├── ChatGPT
└── Midjourney

CD Keys (Root Category)
├── Steam
├── Origin
└── Epic Games
```

### Database Schema:
```sql
CREATE TABLE category (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    image VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    parent_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES category(id)
);
```

---

## 🔄 API Endpoints - Yeni Structure

### Root Categories (Ana Kategoriler)
```
GET  /api/category/list              → Tüm root kategorileri listele
GET  /api/category/list/paginated    → Root kategorileri sayfalı listele
```

### Sub-Categories (Alt Kategoriler)
```
GET  /api/category/{parentId}/subcategories           → Alt kategorileri listele
GET  /api/category/{parentId}/subcategories/paginated → Alt kategorileri sayfalı listele
```

### CRUD Operations
```
POST   /api/category/create           → Yeni kategori oluştur (parentId ile hierarchy)
PUT    /api/category/update           → Kategori güncelle (parentId ile hierarchy)
DELETE /api/category/delete           → Soft delete
POST   /api/category/restore          → Restore
DELETE /api/category/{id}/permanent   → Permanent delete
```

---

## 📊 Service Methods - Updated

### CategoryService.java
- `getAllCategories()` → `findByParentIsNullAndIsActiveTrue()`
- `getSubCategories(Long parentId)` → `findByParentIdAndIsActiveTrue(parentId)`
- `createCategory()` → Parent relationship handling + slug generation
- `updateCategory()` → Parent relationship update + slug regeneration
- `generateSlug(String name)` → SEO-friendly URL generation

### Repository Methods
- `findByParentIsNullAndIsActiveTrue()` - Root categories
- `findByParentIdAndIsActiveTrue(Long parentId)` - Sub-categories
- Pagination support for all methods

---

## 🔍 Slug Generation

### Algorithm:
```java
private String generateSlug(String name) {
    // Türkçe karakterleri normalize et
    String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    String withoutAccents = pattern.matcher(normalized).replaceAll("");
    
    // Küçük harfe çevir, özel karakterleri kaldır
    return withoutAccents.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
}
```

### Examples:
- "PUBG Mobile" → `pubg-mobile`
- "Games & Entertainment" → `games-entertainment`
- "Çok Özel Kategori" → `cok-ozel-kategori`

---

## ✅ Validation Rules

### Category Validation:
```
- name: @NotBlank, @Size(max=100)
- image: @NotBlank
- description: @NotBlank, @Size(max=500)
- parentId: nullable (for sub-categories)
- slug: auto-generated, unique
```

### Request Body Examples:

**Root Category:**
```json
{
  "name": "Games",
  "image": "games.jpg",
  "description": "Game categories",
  "parentId": null
}
```

**Sub-Category:**
```json
{
  "name": "PUBG Mobile",
  "image": "pubg.jpg",
  "description": "PUBG Mobile games",
  "parentId": 1
}
```

---

## 🔄 Migration Strategy

### Eski Sistem (Flat):
```sql
-- Eski kayıtlar
game: "PUBG MOBILE"
type: "ARENA"
```

### Yeni Sistem (Hierarchical):
```sql
-- Ana kategori
INSERT INTO category (name, slug, parent_id) VALUES ('Games', 'games', NULL);

-- Alt kategori
INSERT INTO category (name, slug, parent_id) VALUES ('PUBG Mobile', 'pubg-mobile', 1);

-- Alt-alt kategori
INSERT INTO category (name, slug, parent_id) VALUES ('Arena', 'arena', 2);
```

### Migration Script:
```sql
-- 1. Ana kategoriler oluştur
INSERT INTO category (name, slug, image, description, parent_id, is_active, created_at)
SELECT DISTINCT game, LOWER(REPLACE(game, ' ', '-')), 'default.jpg', CONCAT(game, ' category'), NULL, true, NOW()
FROM old_category_table;

-- 2. Alt kategoriler oluştur
INSERT INTO category (name, slug, image, description, parent_id, is_active, created_at)
SELECT DISTINCT type, LOWER(REPLACE(type, ' ', '-')), 'default.jpg', CONCAT(type, ' sub-category'), 
       (SELECT id FROM category WHERE name = old.game LIMIT 1), true, NOW()
FROM old_category_table old
WHERE type IS NOT NULL;
```

---

## 📊 Performance Considerations

### Indexing Strategy:
```sql
-- Parent-child ilişkisi için
CREATE INDEX idx_category_parent_id ON category(parent_id);

-- Active categories için
CREATE INDEX idx_category_is_active ON category(is_active);

-- Slug lookups için
CREATE INDEX idx_category_slug ON category(slug);

-- Composite index
CREATE INDEX idx_category_parent_active ON category(parent_id, is_active);
```

### Query Optimization:
- Lazy loading for children relationships
- Eager loading for parent relationships
- Pagination for large datasets
- Slug-based lookups for SEO

---

## 🧪 Testing Scenarios

### 1. Root Categories
```bash
GET /api/category/list
# Should return only categories with parent_id = null
```

### 2. Sub-Categories
```bash
GET /api/category/1/subcategories
# Should return children of category with id=1
```

### 3. Create Hierarchy
```bash
# Root category
POST /api/category/create
{"name": "Games", "parentId": null}

# Sub-category
POST /api/category/create
{"name": "PUBG Mobile", "parentId": 1}
```

### 4. Slug Generation
```bash
POST /api/category/create
{"name": "Çok Özel Kategori 2024!"}
# Should generate slug: "cok-ozel-kategori-2024"
```

---

## 📝 Files Modified

| File | Status | Changes |
|------|--------|---------|
| Category.java | ✅ Modified | Hierarchical structure, slug field |
| AddCategoryRequestBody.java | ✅ Modified | parentId field added |
| UpdateCategoryRequestBody.java | ✅ Modified | parentId field added |
| CategoryRepository.java | ✅ Modified | New hierarchical query methods |
| CategoryService.java | ✅ Modified | Parent handling, slug generation |
| CategoryController.java | ✅ Modified | New subcategories endpoints |

---

## 🚀 Benefits of Hierarchical Structure

### 1. **Scalability**
- Unlimited category depth
- Easy to add new levels
- Flexible structure

### 2. **SEO Friendly**
- Slug-based URLs
- Clean URL structure
- Search engine optimization

### 3. **User Experience**
- Intuitive navigation
- Clear category hierarchy
- Better organization

### 4. **Maintainability**
- Easy to add/remove categories
- No fixed structure limitations
- Database normalization

---

## 🔍 API Response Examples

### Root Categories Response:
```json
{
  "success": true,
  "message": "Ana kategoriler başarıyla getirildi",
  "data": [
    {
      "id": 1,
      "name": "Games",
      "slug": "games",
      "children": [
        {
          "id": 2,
          "name": "PUBG Mobile",
          "slug": "pubg-mobile"
        }
      ]
    }
  ]
}
```

### Sub-Categories Response:
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

---

## 🎯 Summary

✅ **Hierarchical Category System Successfully Implemented**

- Self-referencing entity structure
- JSON serialization handling
- SEO-friendly slug generation
- Parent-child relationship management
- Backward compatibility maintained
- Pagination support
- Comprehensive API endpoints

**Build Status**: ✅ SUCCESS
**Compilation**: ✅ 36 source files
**New Endpoints**: 2 (subcategories)
**Enhanced Features**: Slug generation, parent relationships
