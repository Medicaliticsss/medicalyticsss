package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.dto.PasswordChangeDto;
import com.medicalyticsss.backend.model.User;
import com.medicalyticsss.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingsController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PutMapping("/password")
    public ResponseEntity<String> changePassword(@RequestBody PasswordChangeDto dto, Authentication authentication) {

        // Upewniamy sie, ze użytkownik jest zalogowany
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Błąd: Brak autoryzacji.");
        }
        // Pobieramy nazwe zalogowanego użytkownika z sesji i szukamy go w bazie
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Błąd: Nie znaleziono użytkownika.");
        }

        User user = userOpt.get();

        // Weryfikacja czy podane stare hasło zgadza się z hashem w bazie
        if (!passwordEncoder.matches(dto.oldPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(403).body("Błąd: Aktualne hasło jest niepoprawne!");
        }

        // Szyfrowanie i zapis nowego hasla
        String newEncodedPassword = passwordEncoder.encode(dto.newPassword());
        user.setPasswordHash(newEncodedPassword);
        userRepository.save(user);

        return ResponseEntity.ok("Hasło zostało pomyślnie zmienione!");
    }
}