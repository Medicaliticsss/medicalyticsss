package com.medicalyticsss.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")

public class User {

    @Id // Klucz główny tabeli
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Autoinkrementacja
    private Long id;

    @Column(nullable = false, unique = true) // Kolumna NOT NULL, UNIQUE
    private String username;

    @Column(name = "password_hash", nullable = false) // Kolumna password_hash w bazie danych, w kodzie passwordHash, NOT NULL
    private String passwordHash;

    // Pusty konstruktor bo jest wymagany (cdn)
    public User() {
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    // Gettery i Settery
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}