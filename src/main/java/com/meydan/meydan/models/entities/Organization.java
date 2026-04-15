package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // oid ekle
    @Generated(event = EventType.INSERT)
    @Column(columnDefinition = "uuid DEFAULT gen_random_uuid()", insertable = false, updatable = false)
    private UUID oid;
    @Column(name = "category_id")
    private Long categoryId;
    @Column(nullable = false, unique = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
