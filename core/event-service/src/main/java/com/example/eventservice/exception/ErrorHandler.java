package com.example.eventservice.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ErrorHandler {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiError {
        private String status;
        private String reason;
        private String message;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime timestamp;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException ex) {
        return build("NOT_FOUND", "The required object was not found.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException ex) {
        return build("CONFLICT", "Integrity constraint has been violated.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleForbidden(ForbiddenOperationException ex) {
        return build("FORBIDDEN", "For the requested operation the conditions are not met.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConditionsNotMet(ConditionsNotMetException ex) {
        return build("CONFLICT", "For the requested operation the conditions are not met.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrity(DataIntegrityViolationException ex) {
        return build("CONFLICT", "Integrity constraint has been violated.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(BadRequestException ex) {
        return build("BAD_REQUEST", "Incorrectly made request.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return build("BAD_REQUEST", "Incorrectly made request.", msg);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParam(MissingServletRequestParameterException ex) {
        return build("BAD_REQUEST", "Incorrectly made request.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleDateTimeParse(DateTimeParseException ex) {
        return build("BAD_REQUEST", "Incorrectly made request.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(IllegalArgumentException ex) {
        return build("BAD_REQUEST", "Incorrectly made request.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex) {
        return build("BAD_REQUEST", "Incorrectly made request.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build("BAD_REQUEST", "Incorrectly made request.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleNotReadable(HttpMessageNotReadableException ex) {
        return build("BAD_REQUEST", "Incorrectly made request.", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGeneral(Exception ex) {
        return build("INTERNAL_SERVER_ERROR", "An unexpected error occurred.", ex.getMessage());
    }

    private ApiError build(String status, String reason, String message) {
        return ApiError.builder().status(status).reason(reason).message(message).timestamp(LocalDateTime.now()).build();
    }
}
