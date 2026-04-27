package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.model.User;
import com.medicalyticsss.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username, @RequestParam String password) {

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Błąd: Użytkownik o takiej nazwie już istnieje!");
        }

        String encodedPassword = passwordEncoder.encode(password);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(encodedPassword);
        userRepository.save(newUser);

        return ResponseEntity.ok("Zarejestrowano pomyślnie!");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password, HttpServletRequest request) {

        return userRepository.findByUsername(username)
                .map(user -> {
                    // Jeśli hasło z bazy zgadza się z podanym
                    if (passwordEncoder.matches(password, user.getPasswordHash())) {

                        // 1. Tworzymy token autoryzacji
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());

                        // 2. Umieszczamy token w kontekście bezpieczeństwa
                        SecurityContext sc = SecurityContextHolder.getContext();
                        sc.setAuthentication(authToken);

                        // 3. Wymuszamy utworzenie sesji HTTP (to wygeneruje ciasteczko JSESSIONID)
                        HttpSession session = request.getSession(true);
                        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

                        return ResponseEntity.ok("Zalogowano pomyślnie!");
                    } else {
                        // Hasło nie pasuje - błąd 401
                        return ResponseEntity.status(401).body("Błędne login lub hasło!");
                    }
                })
                // Użytkownika nie ma w bazie - błąd 401
                .orElse(ResponseEntity.status(401).body("Nie znaleziono użytkownika!"));
    }

    // NOWY ENDPOINT: SPRAWDZENIE SESJI
    @GetMapping("/me")
    public ResponseEntity<String> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Błąd: Brak aktywnej sesji.");
        }
        return ResponseEntity.ok("Zalogowany jako: " + authentication.getName());
    }

    // NOWY ENDPOINT: WYLOGOWANIE
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // Pobiera sesję tylko jeśli istnieje
        if (session != null) {
            session.invalidate(); // Niszczy sesję
        }
        SecurityContextHolder.clearContext(); // Czyści kontekst
        return ResponseEntity.ok("Wylogowano pomyślnie.");
    }
}