package com.example.requestservice.controller;

import com.example.requestservice.dto.ParticipationRequestDto;
import com.example.requestservice.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getByUser(@PathVariable Long userId) {
        return requestService.getByUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto create(
        @PathVariable Long userId,
        @RequestParam Long eventId
    ) {
        return requestService.create(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancel(
        @PathVariable Long userId,
        @PathVariable Long requestId
    ) {
        return requestService.cancel(userId, requestId);
    }
}
