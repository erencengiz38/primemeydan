package com.meydan.meydan.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.meydan.meydan.models.entities.MediaAsset;
import com.meydan.meydan.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final MediaAssetRepository mediaAssetRepository;

    public MediaAsset uploadAndSaveImage(MultipartFile file, String assetType, String relatedId) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

        String imageId = uploadResult.get("public_id").toString();
        String imageUrl = uploadResult.get("secure_url").toString();

        MediaAsset mediaAsset = MediaAsset.builder()
                .imageId(imageId)
                .imageUrl(imageUrl)
                .assetType(assetType)
                .relatedId(relatedId)
                .build();

        return mediaAssetRepository.save(mediaAsset);
    }

    public void deleteImageFromCloudinary(String imageId) throws IOException {
        cloudinary.uploader().destroy(imageId, ObjectUtils.emptyMap());
    }
}