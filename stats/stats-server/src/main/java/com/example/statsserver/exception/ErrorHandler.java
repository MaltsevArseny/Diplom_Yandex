package com.example.statsserver.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

        private LocalDateTime timestamp;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParam(MissingServletRequestParameterException ex) {
        return ApiError.builder()
            .status("BAD_REQUEST")
            .reason("Incorrectly made request.")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleNotReadable(HttpMessageNotReadableException ex) {
        return ApiError.builder()
            .status("BAD_REQUEST")
            .reason("Incorrectly made request.")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(BadRequestException ex) {
        return ApiError.builder()
            .status("BAD_REQUEST")
            .reason("Incorrectly made request.")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGeneral(Exception ex) {
        return ApiError.builder()
            .status("INTERNAL_SERVER_ERROR")
            .reason("An unexpected error occurred.")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }
}
