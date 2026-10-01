package com.example.notification.dto;

public record NotificationPreferenceResponse(
		boolean emailEnabled,
		boolean smsEnabled,
		boolean pushEnabled) {
}