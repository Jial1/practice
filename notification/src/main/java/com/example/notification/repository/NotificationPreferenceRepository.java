package com.example.notification.repository;

import com.example.notification.persistence.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, Long> {
	List<NotificationPreferenceEntity> findAllByUserId(String userId);
}