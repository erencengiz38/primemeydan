package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    List<MediaAsset> findByAssetTypeAndRelatedId(String assetType, String relatedId);
}