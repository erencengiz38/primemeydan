package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass // Bu anatasyon çok kritik: "Bu bir tablo değil, tabloların babasıdır" der.
@Data
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Güvenlik ve URL'ler için UUID kullanmak iyidir (Dışarıya gerçek ID'yi göstermeyiz)
    @Column(unique = true, nullable = false, updatable = false)
    private String oid = UUID.randomUUID().toString();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}