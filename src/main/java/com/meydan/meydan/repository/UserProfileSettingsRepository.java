package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.UserProfileSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileSettingsRepository extends JpaRepository<UserProfileSettings, Long> {
    Optional<UserProfileSettings> findByUserId(Long userId);
}

