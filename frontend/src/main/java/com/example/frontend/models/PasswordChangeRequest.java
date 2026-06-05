package com.example.frontend.models;

public record PasswordChangeRequest(String oldPassword, String newPassword) {
}
