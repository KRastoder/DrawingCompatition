package com.drawing_compatition.KRastoder.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        log.warn("API exception: {} - Path: {} - RequestId: {}", 
                exception.getMessage(), request.getRequestURI(), getRequestId());
        return problem(exception.getStatus(), exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        log.warn("Authentication failed: {} - Path: {} - RequestId: {}", 
                exception.getMessage(), request.getRequestURI(), getRequestId());
        return problem(HttpStatus.UNAUTHORIZED, "Invalid username or password.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        log.warn("Validation failed: {} - Path: {} - RequestId: {}", 
                errors, request.getRequestURI(), getRequestId());

        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, "Please check the submitted values.", request);
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("Constraint violation: {} - Path: {} - RequestId: {}", 
                exception.getMessage(), request.getRequestURI(), getRequestId());
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String name = exception.getName();
        if ("date".equals(name) || "from".equals(name) || "to".equals(name)) {
            return problem(HttpStatus.BAD_REQUEST, name + " must be a valid date in YYYY-MM-DD format.", request);
        }
        log.warn("Type mismatch for parameter '{}': {} - Path: {} - RequestId: {}", 
                name, exception.getValue(), request.getRequestURI(), getRequestId());
        return problem(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + name + "'.", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleUploadTooLarge(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        log.warn("Upload size exceeded - Path: {} - RequestId: {}", 
                request.getRequestURI(), getRequestId());
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded image must be 10 MB or smaller.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        log.warn("Unsupported media type: {} - Path: {} - RequestId: {}", 
                exception.getContentType(), request.getRequestURI(), getRequestId());
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "That content type is not supported.", request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ProblemDetail> handleUnreadableRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        log.warn("Unreadable request: {} - Path: {} - RequestId: {}", 
                exception.getMessage(), request.getRequestURI(), getRequestId());
        return problem(HttpStatus.BAD_REQUEST, "The request body is missing or invalid.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.error("Data integrity violation: {} - Path: {} - RequestId: {}", 
                exception.getMostSpecificCause() != null ? exception.getMostSpecificCause().getMessage() : exception.getMessage(),
                request.getRequestURI(), getRequestId(), exception);
        return problem(HttpStatus.CONFLICT, "That change conflicts with existing data.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: {} - Path: {} - RequestId: {}", 
                exception.getResourcePath(), request.getRequestURI(), getRequestId());
        return problem(HttpStatus.NOT_FOUND, "The requested resource was not found.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled request failure - Path: {} - RequestId: {}", 
                request.getRequestURI(), getRequestId(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String detail,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(createProblem(status, detail, request));
    }

    private ProblemDetail createProblem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("requestId", getRequestId());
        return problem;
    }

    private String getRequestId() {
        return MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
    }
}
