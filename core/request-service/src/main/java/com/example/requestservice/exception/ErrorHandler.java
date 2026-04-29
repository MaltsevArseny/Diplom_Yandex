package com.example.requestservice.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.time.LocalDateTime;

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
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleServiceUnavailable(ServiceUnavailableException ex) {
        return ApiError.builder().status("SERVICE_UNAVAILABLE").reason("Downstream service is unavailable.")
            .message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex) {
        return ApiError.builder().status("BAD_REQUEST").reason("Incorrectly made request.")
            .message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParam(MissingServletRequestParameterException ex) {
        return ApiError.builder().status("BAD_REQUEST").reason("Incorrectly made request.")
            .message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGeneral(Exception ex) {
        return ApiError.builder().status("INTERNAL_SERVER_ERROR").reason("An unexpected error occurred.")
            .message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }
}
