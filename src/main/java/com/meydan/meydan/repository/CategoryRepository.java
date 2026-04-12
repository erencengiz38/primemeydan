package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.Category;
import com.meydan.meydan.models.enums.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNullAndIsActiveTrue();

    Page<Category> findByParentIsNullAndIsActiveTrue(Pageable pageable);

    // Yeni eklenen Type filtreli metotlar
    List<Category> findByTypeAndParentIsNullAndIsActiveTrue(CategoryType type);

    Page<Category> findByTypeAndParentIsNullAndIsActiveTrue(CategoryType type, Pageable pageable);

    List<Category> findByParentIdAndIsActiveTrue(Long parentId);

    Page<Category> findByParentIdAndIsActiveTrue(Long parentId, Pageable pageable);

    Optional<Category> findBySlug(String slug);
}
