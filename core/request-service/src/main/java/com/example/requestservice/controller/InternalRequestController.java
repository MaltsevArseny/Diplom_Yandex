package com.example.requestservice.controller;

import com.example.requestservice.dto.EventRequestStatusUpdateRequest;
import com.example.requestservice.dto.EventRequestStatusUpdateResult;
import com.example.requestservice.dto.ParticipationRequestDto;
import com.example.requestservice.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/events")
    public List<ParticipationRequestDto> getByEventId(@RequestParam Long eventId) {
        return requestService.getByEventId(eventId);
    }

    @PostMapping("/status-update")
    public EventRequestStatusUpdateResult updateStatuses(
        @RequestParam Long eventId,
        @RequestParam Integer participantLimit,
        @RequestParam Long currentConfirmedCount,
        @RequestBody EventRequestStatusUpdateRequest updateRequest
    ) {
        return requestService.updateStatuses(eventId, participantLimit, currentConfirmedCount, updateRequest);
    }

    @GetMapping("/confirmed-count")
    public Map<Long, Long> getConfirmedCounts(@RequestParam List<Long> eventIds) {
        return requestService.getConfirmedCounts(eventIds);
    }
}
