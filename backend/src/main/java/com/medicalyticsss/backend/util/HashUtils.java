package com.medicalyticsss.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    // Metoda zamieniająca tekst (tu PESEL) na Hash SHA-256
    public static String generateHash(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Wartość do hashowania nie może być pusta");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Zamiana bajtow na format szesnastkowy
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Błąd krytyczny serwera: Brak algorytmu SHA-256", e);
        }
    }
}