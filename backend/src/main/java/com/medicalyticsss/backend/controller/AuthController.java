package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.model.User;
import com.medicalyticsss.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController // Adnotacja o klasie odbierającej zapytania HTTP
@RequestMapping("/api/auth") // Wszystko tutaj będzie miało adres zaczynający się od /api/auth

public class AuthController {

    @Autowired // Znajduje gotowy obiekt UserRepository
    private UserRepository userRepository;

    @PostMapping("/login") // Metoda uruchomi się gdy ktoś wyśle żądanie POST na /api/auth/login.

    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {

        // 1. Szuka użytkownika po zmiennej username
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 2. Sprawdza hasło po zmiennej password
            if (user.getPasswordHash().equals(password)) {
                return ResponseEntity.ok("Zalogowano pomyślnie!");
            }
        }

        return ResponseEntity.status(401).body("Błędny login lub hasło");
    }
}