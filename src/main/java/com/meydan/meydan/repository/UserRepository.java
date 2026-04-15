package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.models.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMail(String mail);
    boolean existsByMail(String mail);
    boolean existsByRole(Role role);

    @Query("SELECT u FROM users u WHERE NOT EXISTS (" +
           "SELECT 1 FROM ClanMember cm " +
           "WHERE cm.userId = u.id AND cm.categoryId = :categoryId AND cm.isActive = true)")
    Page<User> findUsersNotInClanByCategory(@Param("categoryId") Long categoryId, Pageable pageable);
}