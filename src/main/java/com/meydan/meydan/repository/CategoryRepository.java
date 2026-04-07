package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.Category;
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

    List<Category> findByParentIdAndIsActiveTrue(Long parentId);

    Page<Category> findByParentIdAndIsActiveTrue(Long parentId, Pageable pageable);

    Optional<Category> findBySlug(String slug);
}