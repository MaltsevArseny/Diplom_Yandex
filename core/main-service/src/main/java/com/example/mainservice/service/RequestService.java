package com.example.mainservice.service;

import com.example.mainservice.dto.ParticipationRequestDto;
import com.example.mainservice.exception.ConflictException;
import com.example.mainservice.exception.NotFoundException;
import com.example.mainservice.model.Event;
import com.example.mainservice.model.EventState;
import com.example.mainservice.model.ParticipationRequest;
import com.example.mainservice.model.RequestStatus;
import com.example.mainservice.model.User;
import com.example.mainservice.repository.EventRepository;
import com.example.mainservice.repository.RequestRepository;
import com.example.mainservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;

    private final UserRepository userRepository;

    private final EventRepository eventRepository;

    public List<ParticipationRequestDto> getByUser(Long userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        return requestRepository.findAllByRequesterId(userId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Request already exists");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request their own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event is not published");
        }
        if (event.getParticipantLimit() != 0
            && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }
        RequestStatus status = (!event.getRequestModeration() || event.getParticipantLimit() == 0)
            ? RequestStatus.CONFIRMED
            : RequestStatus.PENDING;
        ParticipationRequest request = ParticipationRequest.builder()
            .created(LocalDateTime.now())
            .event(event)
            .requester(user)
            .status(status)
            .build();
        if (status == RequestStatus.CONFIRMED) {
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }
        return toDto(requestRepository.save(request));
    }

    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
            .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        request.setStatus(RequestStatus.CANCELED);
        return toDto(requestRepository.save(request));
    }

    private ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
            .id(request.getId())
            .created(request.getCreated())
            .event(request.getEvent().getId())
            .requester(request.getRequester().getId())
            .status(request.getStatus().name())
            .build();
    }
}
