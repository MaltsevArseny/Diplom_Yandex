package com.example.eventservice.client;

import com.example.eventservice.dto.EventRequestStatusUpdateRequest;
import com.example.eventservice.dto.EventRequestStatusUpdateResult;
import com.example.eventservice.dto.ParticipationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", fallbackFactory = RequestServiceClientFallbackFactory.class)
public interface RequestServiceClient {

    @GetMapping("/internal/requests/events")
    List<ParticipationRequestDto> getByEventId(@RequestParam Long eventId);

    @PostMapping("/internal/requests/status-update")
    EventRequestStatusUpdateResult updateStatuses(
        @RequestParam Long eventId,
        @RequestParam Integer participantLimit,
        @RequestParam Long currentConfirmedCount,
        @RequestBody EventRequestStatusUpdateRequest updateRequest
    );

    @GetMapping("/internal/requests/confirmed-count")
    Map<Long, Long> getConfirmedCounts(@RequestParam List<Long> eventIds);
}
