package com.example.requestservice.service;

import com.example.requestservice.client.EventServiceClient;
import com.example.requestservice.dto.EventForRequestDto;
import com.example.requestservice.dto.EventRequestStatusUpdateRequest;
import com.example.requestservice.dto.EventRequestStatusUpdateResult;
import com.example.requestservice.dto.ParticipationRequestDto;
import com.example.requestservice.exception.ConflictException;
import com.example.requestservice.exception.NotFoundException;
import com.example.requestservice.exception.ServiceUnavailableException;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import com.example.requestservice.model.ParticipationRequest;
import com.example.requestservice.model.RequestStatus;
import com.example.requestservice.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final EventServiceClient eventServiceClient;
    private final CollectorClient collectorClient;

    public List<ParticipationRequestDto> getByUser(Long userId) {
        return requestRepository.findAllByRequesterId(userId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        EventForRequestDto event = eventServiceClient.getEventById(eventId);
        if (event == null) {
            throw new ServiceUnavailableException("Event service is unavailable");
        }
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Request already exists");
        }
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Initiator cannot request their own event");
        }
        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Event is not published");
        }
        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() != 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }
        RequestStatus status = (!event.getRequestModeration() || event.getParticipantLimit() == 0)
            ? RequestStatus.CONFIRMED
            : RequestStatus.PENDING;

        ParticipationRequest request = ParticipationRequest.builder()
            .created(LocalDateTime.now())
            .eventId(eventId)
            .requesterId(userId)
            .status(status)
            .build();
        request = requestRepository.save(request);

        if (status == RequestStatus.CONFIRMED) {
            try {
                eventServiceClient.updateConfirmedRequests(eventId, 1);
            } catch (Exception e) {
                log.warn("Could not update confirmedRequests for event {}: {}", eventId, e.getMessage());
            }
        }

        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);

        return toDto(request);
    }

    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
            .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        if (request.getStatus() == RequestStatus.CONFIRMED) {
            throw new ConflictException("Cannot cancel already accepted participation request");
        }
        RequestStatus prevStatus = request.getStatus();
        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        if (prevStatus == RequestStatus.CONFIRMED) {
            try {
                eventServiceClient.updateConfirmedRequests(request.getEventId(), -1);
            } catch (Exception e) {
                log.warn("Could not decrement confirmedRequests for event {}: {}", request.getEventId(), e.getMessage());
            }
        }
        return toDto(request);
    }

    public List<ParticipationRequestDto> getByEventId(Long eventId) {
        return requestRepository.findAllByEventId(eventId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult updateStatuses(
        Long eventId,
        Integer participantLimit,
        Long currentConfirmedCount,
        EventRequestStatusUpdateRequest updateRequest
    ) {
        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        long confirmedCount = currentConfirmedCount;

        for (ParticipationRequest req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (participantLimit != 0 && confirmedCount >= participantLimit) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(toDto(req));
                } else {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmedCount++;
                    confirmed.add(toDto(req));
                }
            } else {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(toDto(req));
            }
            requestRepository.save(req);
        }
        return EventRequestStatusUpdateResult.builder()
            .confirmedRequests(confirmed)
            .rejectedRequests(rejected)
            .build();
    }

    public Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
        List<Object[]> rows = requestRepository.countByEventIds(eventIds, RequestStatus.CONFIRMED);
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private ParticipationRequestDto toDto(ParticipationRequest r) {
        return ParticipationRequestDto.builder()
            .id(r.getId())
            .created(r.getCreated())
            .event(r.getEventId())
            .requester(r.getRequesterId())
            .status(r.getStatus().name())
            .build();
    }
}
