package com.example.eventservice.client;

import com.example.eventservice.dto.EventRequestStatusUpdateRequest;
import com.example.eventservice.dto.EventRequestStatusUpdateResult;
import com.example.eventservice.dto.ParticipationRequestDto;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RequestServiceClientFallback implements RequestServiceClient {

    @Override
    public List<ParticipationRequestDto> getByEventId(Long eventId) {
        return Collections.emptyList();
    }

    @Override
    public EventRequestStatusUpdateResult updateStatuses(
        Long eventId, Integer participantLimit, Long currentConfirmedCount,
        EventRequestStatusUpdateRequest updateRequest
    ) {
        return EventRequestStatusUpdateResult.builder()
            .confirmedRequests(Collections.emptyList())
            .rejectedRequests(Collections.emptyList())
            .build();
    }

    @Override
    public Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
        return Collections.emptyMap();
    }
}
