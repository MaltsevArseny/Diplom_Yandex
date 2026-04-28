package com.example.eventservice.controller;

import com.example.eventservice.dto.EventForRequestDto;
import com.example.eventservice.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventForRequestDto getEventForRequest(@PathVariable Long eventId) {
        return eventService.getEventForRequest(eventId);
    }

    @PatchMapping("/{eventId}/confirmed-requests")
    public void updateConfirmedRequests(@PathVariable Long eventId, @RequestParam int delta) {
        eventService.updateConfirmedRequests(eventId, delta);
    }
}
