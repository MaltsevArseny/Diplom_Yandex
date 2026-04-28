package com.example.requestservice.client;

import com.example.requestservice.dto.EventForRequestDto;
import org.springframework.stereotype.Component;

@Component
public class EventServiceClientFallback implements EventServiceClient {

    @Override
    public EventForRequestDto getEventById(Long eventId) {
        return null;
    }

    @Override
    public void updateConfirmedRequests(Long eventId, int delta) {
        // fallback: ignore the update, event-service will reconcile later
    }
}
