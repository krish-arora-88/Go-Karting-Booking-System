package com.gokarting.adapter.out.persistence.entity;

import com.gokarting.domain.model.User;
import com.gokarting.domain.model.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public User toDomain() {
        return new User(id, username, email, passwordHash, role, createdAt);
    }

    public static UserEntity fromDomain(User u) {
        return UserEntity.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .passwordHash(u.getPasswordHash())
                .role(u.getRole())
                .createdAt(u.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }
}
