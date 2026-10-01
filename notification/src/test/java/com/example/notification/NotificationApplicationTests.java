package com.example.notification;

import com.example.notification.controller.ApiExceptionHandler;
import com.example.notification.controller.NotificationPreferenceController;
import com.example.notification.domain.NotificationChannel;
import com.example.notification.persistence.NotificationPreferenceEntity;
import com.example.notification.repository.NotificationPreferenceRepository;
import com.example.notification.service.NotificationPreferenceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class NotificationApplicationTests {
	@Autowired
	private NotificationPreferenceRepository preferenceRepository;

	@BeforeEach
	void clearPreferences() {
		preferenceRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void getPreferencesReturnsEnabledDefaults() throws Exception {
		mockMvc().perform(get("/users/me/notification-preferences").principal(() -> "user-123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailEnabled").value(true))
				.andExpect(jsonPath("$.smsEnabled").value(true))
				.andExpect(jsonPath("$.pushEnabled").value(true));
	}

	@Test
	void getPreferencesOverlaysSavedChannelValuesOnDefaults() throws Exception {
		preferenceRepository.save(new NotificationPreferenceEntity("user-123", NotificationChannel.SMS, false));

		mockMvc().perform(get("/users/me/notification-preferences").principal(() -> "user-123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailEnabled").value(true))
				.andExpect(jsonPath("$.smsEnabled").value(false))
				.andExpect(jsonPath("$.pushEnabled").value(true));
	}

	@Test
	void getPreferencesRequiresAuthenticatedPrincipal() throws Exception {
		mockMvc().perform(get("/users/me/notification-preferences"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void patchUpdatesOnlyProvidedPreferencesAndReturnsEffectiveValues() throws Exception {
		preferenceRepository.save(new NotificationPreferenceEntity("user-123", NotificationChannel.EMAIL, false));

		mockMvc().perform(patch("/users/me/notification-preferences")
					.principal(() -> "user-123")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"smsEnabled\":false,\"pushEnabled\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailEnabled").value(false))
				.andExpect(jsonPath("$.smsEnabled").value(false))
				.andExpect(jsonPath("$.pushEnabled").value(false));

		mockMvc().perform(get("/users/me/notification-preferences").principal(() -> "user-123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailEnabled").value(false))
				.andExpect(jsonPath("$.smsEnabled").value(false))
				.andExpect(jsonPath("$.pushEnabled").value(false));
	}

	@Test
	void patchRejectsEmptyNullAndUnknownFields() throws Exception {
		mockMvc().perform(patch("/users/me/notification-preferences")
					.principal(() -> "user-123")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest());

		mockMvc().perform(patch("/users/me/notification-preferences")
					.principal(() -> "user-123")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"smsEnabled\":null}"))
				.andExpect(status().isBadRequest());

		mockMvc().perform(patch("/users/me/notification-preferences")
					.principal(() -> "user-123")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":true}"))
				.andExpect(status().isBadRequest());

		mockMvc().perform(patch("/users/me/notification-preferences")
					.principal(() -> "user-123")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"emailEnabled\":\"true\"}"))
				.andExpect(status().isBadRequest());

		mockMvc().perform(patch("/users/me/notification-preferences")
					.principal(() -> "user-123")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"emailEnabled\":1}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void patchMapsMalformedMissingAndUnsupportedBodiesToHttpErrors() throws Exception {
		mockMvc().perform(patch("/users/me/notification-preferences")
					.principal(() -> "user-123")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{invalid"))
				.andExpect(status().isBadRequest());

		mockMvc().perform(patch("/users/me/notification-preferences")
					.principal(() -> "user-123")
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());

		mockMvc().perform(patch("/users/me/notification-preferences")
					.principal(() -> "user-123")
					.contentType(MediaType.TEXT_PLAIN)
					.content("emailEnabled=false"))
				.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void centralizedHandlerReturnsConflictAndSanitizedServerErrors() {
		ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

		ResponseEntity<ProblemDetail> conflict = exceptionHandler.handleDataIntegrityViolation(
				new DataIntegrityViolationException("database constraint details"));
		Assertions.assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
		Assertions.assertFalse(conflict.getBody().getDetail().contains("database constraint details"));

		ResponseEntity<ProblemDetail> failure = exceptionHandler.handleUnexpected(
				new IllegalStateException("internal implementation detail"));
		Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
		Assertions.assertFalse(failure.getBody().getDetail().contains("internal implementation detail"));
	}

	@Test
	void patchRequiresAuthenticatedPrincipal() throws Exception {
		mockMvc().perform(patch("/users/me/notification-preferences")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"emailEnabled\":false}"))
				.andExpect(status().isUnauthorized());
	}

	private MockMvc mockMvc() {
		return MockMvcBuilders
				.standaloneSetup(new NotificationPreferenceController(
						new NotificationPreferenceService(preferenceRepository)))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

}
