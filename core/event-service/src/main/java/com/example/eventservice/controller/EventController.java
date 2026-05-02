package com.example.eventservice.controller;

import com.example.eventservice.dto.EventFullDto;
import com.example.eventservice.dto.EventRequestStatusUpdateRequest;
import com.example.eventservice.dto.EventRequestStatusUpdateResult;
import com.example.eventservice.dto.EventShortDto;
import com.example.eventservice.dto.NewEventDto;
import com.example.eventservice.dto.ParticipationRequestDto;
import com.example.eventservice.dto.UpdateEventRequest;
import com.example.eventservice.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class EventController {

    private final EventService eventService;

    @GetMapping("/admin/events")
    public List<EventFullDto> getAllByAdmin(
        @RequestParam(required = false) List<Long> users,
        @RequestParam(required = false) List<String> states,
        @RequestParam(required = false) List<Long> categories,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
        @RequestParam(defaultValue = "0") @Min(0) Integer from,
        @RequestParam(defaultValue = "10") @Min(1) Integer size
    ) {
        return eventService.getAllByAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/admin/events/{eventId}")
    public EventFullDto updateByAdmin(@PathVariable Long eventId, @Valid @RequestBody UpdateEventRequest dto) {
        return eventService.updateByAdmin(eventId, dto);
    }

    @GetMapping("/users/{userId}/events")
    public List<EventShortDto> getByUserId(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") Integer from,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        return eventService.getByUserId(userId, from, size);
    }

    @PostMapping("/users/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@PathVariable Long userId, @Valid @RequestBody NewEventDto dto) {
        return eventService.create(userId, dto);
    }

    @GetMapping("/users/{userId}/events/{eventId}")
    public EventFullDto getByIdAndUser(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.getByIdAndUser(userId, eventId);
    }

    @PatchMapping("/users/{userId}/events/{eventId}")
    public EventFullDto updateByUser(
        @PathVariable Long userId, @PathVariable Long eventId,
        @Valid @RequestBody UpdateEventRequest dto
    ) {
        return eventService.updateByUser(userId, eventId, dto);
    }

    @GetMapping("/events/recommendations")
    public List<EventShortDto> getRecommendations(
        @RequestHeader("X-EWM-USER-ID") Long userId
    ) {
        return eventService.getRecommendations(userId);
    }

    @GetMapping("/events")
    public List<EventShortDto> getAllPublic(
        @RequestParam(required = false) String text,
        @RequestParam(required = false) List<Long> categories,
        @RequestParam(required = false) Boolean paid,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
        @RequestParam(defaultValue = "false") Boolean onlyAvailable,
        @RequestParam(required = false) String sort,
        @RequestParam(defaultValue = "0") @Min(0) Integer from,
        @RequestParam(defaultValue = "10") @Min(1) Integer size,
        HttpServletRequest request
    ) {
        return eventService.getAllPublic(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, request);
    }

    @GetMapping("/events/{id}")
    public EventFullDto getPublicById(
        @PathVariable Long id,
        @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId
    ) {
        return eventService.getPublicById(id, userId);
    }

    @PutMapping("/events/{eventId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likeEvent(
        @PathVariable Long eventId,
        @RequestHeader("X-EWM-USER-ID") Long userId
    ) {
        eventService.likeEvent(userId, eventId);
    }

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequests(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.getRequests(userId, eventId);
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(
        @PathVariable Long userId, @PathVariable Long eventId,
        @RequestBody EventRequestStatusUpdateRequest dto
    ) {
        return eventService.updateRequestStatus(userId, eventId, dto);
    }
}
