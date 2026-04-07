package com.meydan.meydan.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.util.UUID;

@Entity(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(columnDefinition = "uuid DEFAULT gen_random_uuid()", insertable = false, updatable = false)
    private UUID oid;

    private String display_name;
    private String mail;
    private String password;
    private String tag;
}
