package com.example.mainservice.controller;

import com.example.mainservice.dto.EventFullDto;
import com.example.mainservice.dto.EventRequestStatusUpdateRequest;
import com.example.mainservice.dto.EventRequestStatusUpdateResult;
import com.example.mainservice.dto.EventShortDto;
import com.example.mainservice.dto.NewEventDto;
import com.example.mainservice.dto.ParticipationRequestDto;
import com.example.mainservice.dto.UpdateEventRequest;
import com.example.mainservice.service.EventService;
import com.example.mainservice.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    private final RequestService requestService;

    @GetMapping("/admin/events")
    public List<EventFullDto> getAllByAdmin(
        @RequestParam(required = false) List<Long> users,
        @RequestParam(required = false) List<String> states,
        @RequestParam(required = false) List<Long> categories,
        @RequestParam(required = false) String rangeStart,
        @RequestParam(required = false) String rangeEnd,
        @RequestParam(defaultValue = "0") Integer from,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        return eventService.getAllByAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/admin/events/{eventId}")
    public EventFullDto updateByAdmin(
        @PathVariable Long eventId,
        @RequestBody UpdateEventRequest dto
    ) {
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
    public EventFullDto getByIdAndUser(
        @PathVariable Long userId,
        @PathVariable Long eventId
    ) {
        return eventService.getByIdAndUser(userId, eventId);
    }

    @PatchMapping("/users/{userId}/events/{eventId}")
    public EventFullDto updateByUser(
        @PathVariable Long userId,
        @PathVariable Long eventId,
        @RequestBody UpdateEventRequest dto
    ) {
        return eventService.updateByUser(userId, eventId, dto);
    }

    @GetMapping("/events")
    public List<EventShortDto> getAllPublic(
        @RequestParam(required = false) String text,
        @RequestParam(required = false) List<Long> categories,
        @RequestParam(required = false) Boolean paid,
        @RequestParam(required = false) String rangeStart,
        @RequestParam(required = false) String rangeEnd,
        @RequestParam(defaultValue = "false") Boolean onlyAvailable,
        @RequestParam(required = false) String sort,
        @RequestParam(defaultValue = "0") Integer from,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        return eventService.getAllPublic(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
    }

    @GetMapping("/events/{id}")
    public EventFullDto getPublicById(@PathVariable Long id) {
        return eventService.getPublicById(id);
    }

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequests(
        @PathVariable Long userId,
        @PathVariable Long eventId
    ) {
        return eventService.getRequests(userId, eventId);
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(
        @PathVariable Long userId,
        @PathVariable Long eventId,
        @RequestBody EventRequestStatusUpdateRequest dto
    ) {
        return eventService.updateRequestStatus(userId, eventId, dto);
    }

    @GetMapping("/users/{userId}/requests")
    public List<ParticipationRequestDto> getRequestsByUser(@PathVariable Long userId) {
        return requestService.getByUser(userId);
    }

    @PostMapping("/users/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(
        @PathVariable Long userId,
        @RequestParam Long eventId
    ) {
        return requestService.create(userId, eventId);
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(
        @PathVariable Long userId,
        @PathVariable Long requestId
    ) {
        return requestService.cancel(userId, requestId);
    }
}
