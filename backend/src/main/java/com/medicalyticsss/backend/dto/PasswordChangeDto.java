package com.medicalyticsss.backend.dto;

public record PasswordChangeDto(
        String oldPassword,
        String newPassword
) {
}