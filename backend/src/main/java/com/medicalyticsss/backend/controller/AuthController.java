package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.model.User;
import com.medicalyticsss.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController // Adnotacja o klasie odbierającej zapytania HTTP
@RequestMapping("/api/auth") // Wszystko tutaj będzie miało adres zaczynający się od /api/auth

public class AuthController {

    @Autowired // Znajduje gotowy obiekt UserRepository
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Z klasy SecurityConfig

    @PostMapping("/register") // Metoda uruchomi się gdy ktoś wyśle żądanie POST na /api/auth/register.
    public String register(@RequestParam String username, @RequestParam String password) {

        // 1. Sprawdza, czy nazwa użytkownika nie jest już zajęta
        if (userRepository.findByUsername(username).isPresent()) {
            return "Błąd: Użytkownik o takiej nazwie już istnieje!";
        }

        // 2. Hashuje hasło
        String encodedPassword = passwordEncoder.encode(password);

        // 3. Tworzy i zapisuje użytkownika
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(encodedPassword);
        userRepository.save(newUser);

        return "Zarejestrowano pomyślnie!";
    }

    @PostMapping("/login") // Metoda uruchomi się gdy ktoś wyśle żądanie POST na /api/auth/login.
    public String login(@RequestParam String username, @RequestParam String password) {

        // 1. Zwraca użytkownika o podanym loginie
        return userRepository.findByUsername(username)

                // 2. Jeżeli użytkownik znaleziony, to sprawdza hasło
                .map(user -> {
                    if (passwordEncoder.matches(password, user.getPasswordHash())) {
                        return "Zalogowano pomyślnie!";
                    } else {
                        return "Błędne login lub hasło!";
                    }
                })
                .orElse("Nie znaleziono użytkownika!");
    }
}