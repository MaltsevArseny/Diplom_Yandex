package com.example.requestservice.client;

import com.example.requestservice.dto.EventForRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "event-service", fallback = EventServiceClientFallback.class)
public interface EventServiceClient {

    @GetMapping("/internal/events/{eventId}")
    EventForRequestDto getEventById(@PathVariable Long eventId);

    @PatchMapping("/internal/events/{eventId}/confirmed-requests")
    void updateConfirmedRequests(@PathVariable Long eventId, @RequestParam int delta);
}
