package com.example.eventservice.client;

import com.example.eventservice.dto.EventRequestStatusUpdateRequest;
import com.example.eventservice.dto.EventRequestStatusUpdateResult;
import com.example.eventservice.dto.ParticipationRequestDto;
import com.example.eventservice.exception.ConflictException;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class RequestServiceClientFallbackFactory implements FallbackFactory<RequestServiceClient> {

    @Override
    public RequestServiceClient create(Throwable cause) {
        return new RequestServiceClient() {

            @Override
            public List<ParticipationRequestDto> getByEventId(Long eventId) {
                return Collections.emptyList();
            }

            @Override
            public EventRequestStatusUpdateResult updateStatuses(
                Long eventId, Integer participantLimit, Long currentConfirmedCount,
                EventRequestStatusUpdateRequest updateRequest
            ) {
                if (cause instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                    throw new ConflictException(cause.getMessage());
                }
                return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(Collections.emptyList())
                    .rejectedRequests(Collections.emptyList())
                    .build();
            }

            @Override
            public Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
                return Collections.emptyMap();
            }
        };
    }
}
