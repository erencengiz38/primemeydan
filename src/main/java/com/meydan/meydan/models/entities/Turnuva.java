package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "turnuva")
@Data
public class Turnuva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(columnDefinition = "uuid DEFAULT gen_random_uuid()", insertable = false, updatable = false)
    private UUID oid;

    private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    private String title;
    private String description;
    private Date start_date;
    private Date finish_date;
    private Boolean isActive;
    private String imageUrl;
    private String link;
    private String link_type;
    private Double reward_amount;
    private String reward_currency;
    private String player_format;
    @Enumerated(EnumType.STRING)
    private ParticipantType participantType = ParticipantType.SOLO;
}