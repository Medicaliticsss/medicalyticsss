package com.example.frontend.utils;

import java.net.URI;

public final class ApiConfig {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private ApiConfig() {
    }

    public static String getBaseUrl() {
        String fromEnv = System.getenv("MEDICALYTICS_API_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return trimTrailingSlash(fromEnv);
        }

        String fromProperty = System.getProperty("medicalytics.api.url");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return trimTrailingSlash(fromProperty);
        }

        return DEFAULT_BASE_URL;
    }

    public static URI apiUri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(getBaseUrl() + normalizedPath);
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
