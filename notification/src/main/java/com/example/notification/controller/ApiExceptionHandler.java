package com.example.notification.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException exception) {
		HttpStatusCode status = exception.getStatusCode();
		String detail = exception.getReason();
		if (detail == null) {
			detail = status.value() == HttpStatus.UNAUTHORIZED.value()
					? "Authentication is required."
					: "The request could not be completed.";
		}
		return problem(status, detail);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Request body is missing or malformed.");
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
		return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json.");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		logger.warn("Preference update conflicted with a database constraint", exception);
		return problem(HttpStatus.CONFLICT, "The preference update conflicts with existing data. Retry the request.");
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ProblemDetail> handleDataAccess(DataAccessException exception) {
		logger.error("Preference persistence failed", exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
		logger.error("Unexpected request failure", exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}