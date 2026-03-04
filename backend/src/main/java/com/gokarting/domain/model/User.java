package com.gokarting.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain User aggregate — pure Java, zero framework dependencies.
 * Persistence is handled in the adapter layer via a separate UserEntity.
 */
public class User {

    private final UUID id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final Instant createdAt;

    public User(UUID id, String username, String email,
                String passwordHash, UserRole role, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public UUID getId()            { return id; }
    public String getUsername()    { return username; }
    public String getEmail()       { return email; }
    public String getPasswordHash(){ return passwordHash; }
    public UserRole getRole()      { return role; }
    public Instant getCreatedAt()  { return createdAt; }
}
