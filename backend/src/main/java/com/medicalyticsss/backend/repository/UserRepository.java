package com.medicalyticsss.backend.repository;

import com.medicalyticsss.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> { // Repo do zarządzania klasą User, klucz główny w klasie User jest typu Long

    // Na podstawie nazwy tej metody, Spring sam wygeneruje zapytanie:
    // SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

}