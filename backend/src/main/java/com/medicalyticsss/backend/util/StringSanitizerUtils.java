package com.medicalyticsss.backend.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringSanitizerUtils {


     //Czyści tekst: zamienia znaki specjalne na spacje, usuwa podwójne spacje
     //i formatuje do Title Case (np. "laboratorium_alfa" -> "Laboratorium Alfa").

    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String cleaned = input.replaceAll("[_-]", " ");

        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return Arrays.stream(cleaned.split(" "))
                .map(word -> {
                    if (word.isEmpty()) return "";
                    return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                })
                .collect(Collectors.joining(" "));
    }
}