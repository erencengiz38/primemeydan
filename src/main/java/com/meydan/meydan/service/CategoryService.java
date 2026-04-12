package com.meydan.meydan.service;

import com.meydan.meydan.dto.CategoryResponseDTO;
import com.meydan.meydan.models.entities.Category;
import com.meydan.meydan.models.enums.CategoryType;
import com.meydan.meydan.repository.CategoryRepository;
import com.meydan.meydan.request.Category.AddCategoryRequestBody;
import com.meydan.meydan.request.Category.DeleteCategoryRequestBody;
import com.meydan.meydan.request.Category.UpdateCategoryRequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    public List<Category> getAllCategories(CategoryType type) {
        if (type != null) {
            return categoryRepository.findByTypeAndParentIsNullAndIsActiveTrue(type);
        }
        return categoryRepository.findByParentIsNullAndIsActiveTrue();
    }

    public Page<Category> getAllCategoriesWithPagination(CategoryType type, Pageable pageable) {
        if (type != null) {
            return categoryRepository.findByTypeAndParentIsNullAndIsActiveTrue(type, pageable);
        }
        return categoryRepository.findByParentIsNullAndIsActiveTrue(pageable);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı"));
    }

    public List<Category> getSubCategories(Long parentId) {
        return categoryRepository.findByParentIdAndIsActiveTrue(parentId);
    }

    public Page<Category> getSubCategoriesWithPagination(Long parentId, Pageable pageable) {
        return categoryRepository.findByParentIdAndIsActiveTrue(parentId, pageable);
    }

    @Transactional
    public CategoryResponseDTO createCategory(AddCategoryRequestBody addCategoryRequestBody) {
        // 1. DTO'dan Entity'ye çevir
        Category category = modelMapper.map(addCategoryRequestBody, Category.class);
        
        // 2. ID'yi GÜVENLİK AMACIYLA null yap (JPA'nın kesinlikle INSERT yapması için)
        category.setId(null);
        category.setOid(null);

        // 3. Parent category'yi set et (eğer parentId varsa)
        if (addCategoryRequestBody.getParentId() != null) {
            Category parent = categoryRepository.findById(addCategoryRequestBody.getParentId())
                    .orElseThrow(() -> new RuntimeException("Ana kategori bulunamadı"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        // 4. Slug generate et ve benzersiz yap
        category.setSlug(generateUniqueSlug(addCategoryRequestBody.getName(), null));

        // 5. Type set et
        category.setType(addCategoryRequestBody.getType());

        // 6. Veritabanına Kaydet
        Category savedCategory = categoryRepository.save(category);

        // 7. Response DTO'ya dönüştür
        CategoryResponseDTO responseDTO = modelMapper.map(savedCategory, CategoryResponseDTO.class);
        
        if (savedCategory.getParent() != null) {
            responseDTO.setParentId(savedCategory.getParent().getId());
        }

        return responseDTO;
    }

    @Transactional
    public Category updateCategory(UpdateCategoryRequestBody updateCategoryRequestBody) {
        Category category = categoryRepository.findById(updateCategoryRequestBody.getId())
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı"));

        if (Boolean.FALSE.equals(category.getIsActive())) {
            throw new RuntimeException("Silinmiş kategori güncellenemez");
        }

        // Kendisini parent olarak atamaya çalışıyorsa engelle
        if (updateCategoryRequestBody.getParentId() != null && updateCategoryRequestBody.getParentId().equals(category.getId())) {
            throw new RuntimeException("Bir kategori kendisinin üst kategorisi olamaz!");
        }

        // Parent category'yi güncelle (eğer parentId varsa)
        if (updateCategoryRequestBody.getParentId() != null) {
            Category parent = categoryRepository.findById(updateCategoryRequestBody.getParentId())
                    .orElseThrow(() -> new RuntimeException("Ana kategori bulunamadı"));
            
            // Sonsuz döngü kontrolü: Parent'ın parent'ı kendisi olmamalı
            Category tempParent = parent;
            while (tempParent != null) {
                if (tempParent.getId().equals(category.getId())) {
                    throw new RuntimeException("Sonsuz döngü hatası: Bu kategori zaten seçilen üst kategorinin hiyerarşisinde yer alıyor!");
                }
                tempParent = tempParent.getParent();
            }
            
            category.setParent(parent);
        } else {
            category.setParent(null); // Root category yap
        }

        category.setName(updateCategoryRequestBody.getName());
        category.setImage(updateCategoryRequestBody.getImage());
        category.setDescription(updateCategoryRequestBody.getDescription());
        
        if (updateCategoryRequestBody.getType() != null) {
            category.setType(updateCategoryRequestBody.getType());
        }

        // Slug'ı yeniden generate et ve benzersiz yap (isim değiştiyse veya her ihtimale karşı güncelleniyorsa)
        category.setSlug(generateUniqueSlug(category.getName(), category.getId()));

        return categoryRepository.save(category);
    }

    public Category deleteCategory(DeleteCategoryRequestBody request) {
        Category category = categoryRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı"));

        if (Boolean.FALSE.equals(category.getIsActive())) {
            throw new RuntimeException("Kategori zaten silinmiş");
        }

        category.setIsActive(false);

        return categoryRepository.save(category);
    }

    public Category restoreCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı"));

        if (Boolean.TRUE.equals(category.getIsActive())) {
            throw new RuntimeException("Kategori zaten aktif");
        }

        category.setIsActive(true);

        return categoryRepository.save(category);
    }

    public Category permanentlyDeleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı"));

        categoryRepository.delete(category);
        return category;
    }

    private String generateUniqueSlug(String name, Long currentId) {
        String baseSlug = generateSlug(name);
        String finalSlug = baseSlug;
        int counter = 1;

        while (true) {
            Optional<Category> existingCategory = categoryRepository.findBySlug(finalSlug);
            
            // Eğer aynı slug'a sahip bir kategori yoksa VEYA aynı slug'a sahip kategori kendisiyse
            if (existingCategory.isEmpty() || existingCategory.get().getId().equals(currentId)) {
                return finalSlug;
            }

            finalSlug = baseSlug + "-" + counter;
            counter++;
        }
    }

    private String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        // Türkçe karakterleri dönüştür ve normalize et
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = pattern.matcher(normalized).replaceAll("");

        // Küçük harfe çevir, boşlukları tire ile değiştir, özel karakterleri kaldır
        return withoutAccents.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
