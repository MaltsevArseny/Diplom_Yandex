package com.example.userservice.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
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
        return ApiError.builder().status("NOT_FOUND").reason("The required object was not found.")
            .message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException ex) {
        return ApiError.builder().status("CONFLICT").reason("Integrity constraint has been violated.")
            .message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrity(DataIntegrityViolationException ex) {
        return ApiError.builder().status("CONFLICT").reason("Integrity constraint has been violated.")
            .message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ApiError.builder().status("BAD_REQUEST").reason("Incorrectly made request.")
            .message(message).timestamp(LocalDateTime.now()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex) {
        return ApiError.builder().status("BAD_REQUEST").reason("Incorrectly made request.")
            .message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGeneral(Exception ex) {
        return ApiError.builder().status("INTERNAL_SERVER_ERROR").reason("An unexpected error occurred.")
            .message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }
}
