package com.example.requestservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventForRequestDto {

    private Long id;
    private String state;
    private Integer participantLimit;
    private Long confirmedRequests;
    private Long initiatorId;
    private Boolean requestModeration;
}
