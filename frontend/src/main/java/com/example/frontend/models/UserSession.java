package com.example.frontend.models;

public class UserSession {
    private static UserSession instance;
    private String username;

    private UserSession() {} // Prywatny konstruktor (Singleton)

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void login(String username) {
        this.username = username;
    }

    public void logout() {
        this.username = null;
    }

    public String getUsername() {
        return username;
    }

    public boolean isLoggedIn() {
        return username != null;
    }
}
