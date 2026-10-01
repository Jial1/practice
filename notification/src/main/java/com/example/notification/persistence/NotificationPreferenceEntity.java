package com.example.notification.persistence;

import com.example.notification.domain.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_preference", uniqueConstraints = {
		@UniqueConstraint(name = "uq_notification_preference_user_channel", columnNames = { "user_id", "channel" })
})
public class NotificationPreferenceEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private String userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 16)
	@Getter
	private NotificationChannel channel;

	@Column(name = "enabled", nullable = false)
	@Getter
	private boolean enabled;

	public NotificationPreferenceEntity(String userId, NotificationChannel channel, boolean enabled) {
		this.userId = userId;
		this.channel = channel;
		this.enabled = enabled;
	}

	public void updateEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}