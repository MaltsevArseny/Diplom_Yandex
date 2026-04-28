package com.example.eventservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {

    @NotBlank(message = "Annotation cannot be blank")
    @Size(min = 20, max = 2000)
    private String annotation;

    @NotNull(message = "Category cannot be null")
    private Long category;

    @NotBlank(message = "Description cannot be blank")
    @Size(min = 20, max = 7000)
    private String description;

    @NotNull(message = "Event date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull(message = "Location cannot be null")
    private LocationDto location;

    private Boolean paid;

    @PositiveOrZero
    private Integer participantLimit;

    private Boolean requestModeration;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 120)
    private String title;
}
