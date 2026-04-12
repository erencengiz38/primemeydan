package com.meydan.meydan.controller;

import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.dto.CategoryResponseDTO;
import com.meydan.meydan.models.entities.Category;
import com.meydan.meydan.models.enums.CategoryType;
import com.meydan.meydan.request.Category.AddCategoryRequestBody;
import com.meydan.meydan.request.Category.DeleteCategoryRequestBody;
import com.meydan.meydan.request.Category.UpdateCategoryRequestBody;
import com.meydan.meydan.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@Tag(name = "Category", description = "Kategori API endpoint'leri")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "Ana kategorileri listele", description = "Aktif ana kategorileri getirir. İsteğe bağlı olarak 'type' (Örn: GAME) parametresi alabilir.")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories(
            @RequestParam(required = false) CategoryType type) {
        List<Category> categories = categoryService.getAllCategories(type);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ana kategoriler başarıyla getirildi", categories));
    }

    @GetMapping("/list/paginated")
    @Operation(summary = "Ana kategorileri sayfalı olarak listele", description = "Aktif ana kategorileri sayfalı şekilde getirir. İsteğe bağlı olarak 'type' parametresi alabilir.")
    public ResponseEntity<ApiResponse<Page<Category>>> getAllCategoriesWithPagination(
            @RequestParam(required = false) CategoryType type,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Category> categories = categoryService.getAllCategoriesWithPagination(type, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ana kategoriler başarıyla getirildi", categories));
    }

    @GetMapping("/{parentId}/subcategories")
    @Operation(summary = "Alt kategorileri listele", description = "Belirli bir ana kategorinin alt kategorilerini getirir")
    public ResponseEntity<ApiResponse<List<Category>>> getSubCategories(@PathVariable Long parentId) {
        List<Category> categories = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alt kategoriler başarıyla getirildi", categories));
    }

    @GetMapping("/{parentId}/subcategories/paginated")
    @Operation(summary = "Alt kategorileri sayfalı olarak listele", description = "Belirli bir ana kategorinin alt kategorilerini sayfalı şekilde getirir")
    public ResponseEntity<ApiResponse<Page<Category>>> getSubCategoriesWithPagination(
            @PathVariable Long parentId,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Category> categories = categoryService.getSubCategoriesWithPagination(parentId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alt kategoriler başarıyla getirildi", categories));
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "Kategori detaylarını getir", description = "Belirli bir kategorinin detaylarını getirir")
    public ResponseEntity<ApiResponse<Category>> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kategori başarıyla getirildi", category));
    }

    @PostMapping("/create")
    @Operation(summary = "Yeni kategori oluştur", description = "Yeni bir kategori oluşturur (ana kategori veya alt kategori)")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategory(@Valid @RequestBody AddCategoryRequestBody category) {
        CategoryResponseDTO savedCategory = categoryService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Kategori başarıyla oluşturuldu", savedCategory));
    }

    @PutMapping("/update")
    @Operation(summary = "Kategori güncelle", description = "Belirli bir kategoriyi günceller")
    public ResponseEntity<ApiResponse<Category>> updateCategory(@Valid @RequestBody UpdateCategoryRequestBody request) {
        Category updatedCategory = categoryService.updateCategory(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kategori başarıyla güncellendi", updatedCategory));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Kategori sil", description = "Belirli bir kategoriyi soft delete ile siler")
    public ResponseEntity<ApiResponse<Category>> deleteCategory(@Valid @RequestBody DeleteCategoryRequestBody request) {
        Category deletedCategory = categoryService.deleteCategory(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kategori başarıyla silindi", deletedCategory));
    }

    @PostMapping("/restore")
    @Operation(summary = "Kategori geri yükle", description = "Belirli bir kategoriyi aktifleştirir")
    public ResponseEntity<ApiResponse<Category>> restoreCategory(@Valid @RequestBody DeleteCategoryRequestBody categoryRequestBody) {
        Category restoredCategory = categoryService.restoreCategory(categoryRequestBody.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Kategori başarıyla aktifleşti", restoredCategory));
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "Kategoriyi kalıcı olarak sil", description = "Belirli bir kategoriyi veritabanından kalıcı olarak siler")
    public ResponseEntity<ApiResponse<Category>> permanentlyDeleteCategory(@PathVariable Long id) {
        Category deletedCategory = categoryService.permanentlyDeleteCategory(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kategori kalıcı olarak silindi", deletedCategory));
    }
}
