package com.meydan.meydan.controller;

import com.meydan.meydan.models.entities.MediaAsset;
import com.meydan.meydan.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public ResponseEntity<MediaAsset> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assetType") String assetType,
            @RequestParam("relatedId") String relatedId
    ) throws IOException {
        MediaAsset savedAsset = cloudinaryService.uploadAndSaveImage(file, assetType, relatedId);
        return ResponseEntity.ok(savedAsset);
    }
}