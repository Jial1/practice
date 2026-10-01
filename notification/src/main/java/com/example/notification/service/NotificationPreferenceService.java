package com.example.notification.service;

import com.example.notification.domain.NotificationChannel;
import com.example.notification.dto.NotificationPreferenceResponse;
import com.example.notification.persistence.NotificationPreferenceEntity;
import com.example.notification.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationPreferenceService {
	private final NotificationPreferenceRepository preferenceRepository;

	public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
		this.preferenceRepository = preferenceRepository;
	}

	public NotificationPreferenceResponse getPreferences(String userId) {
		Map<NotificationChannel, Boolean> preferences = new EnumMap<>(NotificationChannel.class);
		for (NotificationChannel channel : NotificationChannel.values()) {
			preferences.put(channel, true);
		}
		for (NotificationPreferenceEntity preference : preferenceRepository.findAllByUserId(userId)) {
			preferences.put(preference.getChannel(), preference.isEnabled());
		}

		return new NotificationPreferenceResponse(
				preferences.get(NotificationChannel.EMAIL),
				preferences.get(NotificationChannel.SMS),
				preferences.get(NotificationChannel.PUSH));
	}

	@Transactional
	public NotificationPreferenceResponse updatePreferences(String userId, Map<NotificationChannel, Boolean> updates) {
		Map<NotificationChannel, NotificationPreferenceEntity> existing = new EnumMap<>(NotificationChannel.class);
		for (NotificationPreferenceEntity preference : preferenceRepository.findAllByUserId(userId)) {
			existing.put(preference.getChannel(), preference);
		}

		List<NotificationPreferenceEntity> changed = new ArrayList<>();
		for (Map.Entry<NotificationChannel, Boolean> update : updates.entrySet()) {
			NotificationPreferenceEntity preference = existing.get(update.getKey());
			if (preference == null) {
				preference = new NotificationPreferenceEntity(userId, update.getKey(), update.getValue());
			} else {
				preference.updateEnabled(update.getValue());
			}
			changed.add(preference);
		}
		preferenceRepository.saveAll(changed);

		return getPreferences(userId);
	}
}