package com.example.eventservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {
    private String app;
    private String uri;
    private String ip;
    private String timestamp;
}
