package com.example.notification.controller;

import com.example.notification.domain.NotificationChannel;
import com.example.notification.dto.NotificationPreferencePatchRequest;
import com.example.notification.dto.NotificationPreferenceResponse;
import com.example.notification.service.NotificationPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.EnumMap;
import java.util.Map;

@RestController
@RequestMapping("/users/me/notification-preferences")
public class NotificationPreferenceController {

	private final NotificationPreferenceService preferenceService;

	public NotificationPreferenceController(NotificationPreferenceService preferenceService) {
		this.preferenceService = preferenceService;
	}

	@GetMapping
	public NotificationPreferenceResponse getPreferences(Principal principal) {
		return preferenceService.getPreferences(requireUserId(principal));
	}

	@PatchMapping
	public NotificationPreferenceResponse updatePreferences(
			Principal principal,
			@RequestBody NotificationPreferencePatchRequest request) {
		return preferenceService.updatePreferences(requireUserId(principal), parseUpdates(request));
	}

	private String requireUserId(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		return principal.getName();
	}

	private Map<NotificationChannel, Boolean> parseUpdates(NotificationPreferencePatchRequest request) {
		if (request == null || request.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PATCH must contain at least one preference");
		}

		Map<NotificationChannel, Boolean> updates = new EnumMap<>(NotificationChannel.class);
		for (Map.Entry<String, Object> field : request.entrySet()) {
			NotificationChannel channel = switch (field.getKey()) {
				case "emailEnabled" -> NotificationChannel.EMAIL;
				case "smsEnabled" -> NotificationChannel.SMS;
				case "pushEnabled" -> NotificationChannel.PUSH;
				default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Unknown preference field: " + field.getKey());
			};

			if (!(field.getValue() instanceof Boolean enabled)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Preference values must be booleans: " + field.getKey());
			}
			updates.put(channel, enabled);
		}
		return updates;
	}
}