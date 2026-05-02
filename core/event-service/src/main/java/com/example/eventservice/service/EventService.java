package com.example.eventservice.service;

import com.example.eventservice.client.RequestServiceClient;
import com.example.eventservice.client.UserServiceClient;
import com.example.eventservice.dto.EventForRequestDto;
import com.example.eventservice.dto.EventFullDto;
import com.example.eventservice.dto.EventRequestStatusUpdateRequest;
import com.example.eventservice.dto.EventRequestStatusUpdateResult;
import com.example.eventservice.dto.EventShortDto;
import com.example.eventservice.dto.NewEventDto;
import com.example.eventservice.dto.ParticipationRequestDto;
import com.example.eventservice.dto.UpdateEventRequest;
import com.example.eventservice.dto.UserShortDto;
import com.example.eventservice.exception.BadRequestException;
import com.example.eventservice.exception.ConditionsNotMetException;
import com.example.eventservice.exception.ConflictException;
import com.example.eventservice.exception.ForbiddenOperationException;
import com.example.eventservice.exception.NotFoundException;
import com.example.eventservice.mapper.EventMapper;
import com.example.eventservice.model.Category;
import com.example.eventservice.model.Event;
import com.example.eventservice.model.EventState;
import com.example.eventservice.model.RequestStatus;
import com.example.eventservice.model.StateAction;
import com.example.eventservice.repository.CategoryRepository;
import com.example.eventservice.repository.EventRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.client.AnalyzerClient;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.proto.analyzer.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final int DEFAULT_RECOMMENDATIONS_SIZE = 10;

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final UserServiceClient userServiceClient;
    private final RequestServiceClient requestServiceClient;
    private final CollectorClient collectorClient;
    private final AnalyzerClient analyzerClient;

    public List<EventFullDto> getAllByAdmin(
        List<Long> users, List<String> states, List<Long> categories,
        LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size
    ) {
        List<EventState> statesList = null;
        if (states != null) {
            try {
                statesList = states.stream().map(EventState::valueOf).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid event state");
            }
        }
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }
        final List<EventState> finalStates = statesList;
        final List<Long> catList = (categories != null && !categories.isEmpty()) ? categories : null;

        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (users != null && !users.isEmpty()) predicates.add(root.get("initiatorId").in(users));
            if (finalStates != null && !finalStates.isEmpty()) predicates.add(root.get("state").in(finalStates));
            if (catList != null) predicates.add(root.get("category").get("id").in(catList));
            if (rangeStart != null) predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            if (rangeEnd != null) predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Event> events = eventRepository.findAll(spec, PageRequest.of(from / size, size)).getContent();
        enrichWithRatings(events);
        Map<Long, UserShortDto> usersMap = fetchUsersMap(events);
        return events.stream().map(e -> eventMapper.toFullDto(e, usersMap.getOrDefault(e.getInitiatorId(),
            UserShortDto.builder().id(e.getInitiatorId()).name("").build()))).collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventRequest dto) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        applyUpdate(event, dto);
        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ForbiddenOperationException("Cannot publish the event because it's not in the right state: " + event.getState());
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ForbiddenOperationException("Event date must be at least 1 hour from publication date");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (dto.getStateAction() == StateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ForbiddenOperationException("Cannot reject the event because it's already published");
                }
                event.setState(EventState.CANCELED);
            }
        }
        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Event date must be at least 1 hour from now");
        }
        event = eventRepository.save(event);
        UserShortDto initiator = userServiceClient.getById(event.getInitiatorId());
        return eventMapper.toFullDto(event, initiator);
    }

    public List<EventShortDto> getByUserId(Long userId, Integer from, Integer size) {
        List<Event> events = eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size)).getContent();
        UserShortDto initiator = userServiceClient.getById(userId);
        return events.stream().map(e -> eventMapper.toShortDto(e, initiator)).collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        UserShortDto user = userServiceClient.getById(userId);
        if (user == null || user.getId() == null) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        Category category = categoryRepository.findById(dto.getCategory())
            .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));
        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }
        Event event = Event.builder()
            .annotation(dto.getAnnotation())
            .category(category)
            .confirmedRequests(0L)
            .createdOn(LocalDateTime.now())
            .description(dto.getDescription())
            .eventDate(dto.getEventDate())
            .initiatorId(userId)
            .lat(dto.getLocation() != null ? dto.getLocation().getLat() : null)
            .lon(dto.getLocation() != null ? dto.getLocation().getLon() : null)
            .paid(dto.getPaid() != null ? dto.getPaid() : false)
            .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
            .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
            .state(EventState.PENDING)
            .title(dto.getTitle())
            .rating(0.0)
            .build();
        return eventMapper.toFullDto(eventRepository.save(event), user);
    }

    public EventFullDto getByIdAndUser(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        UserShortDto initiator = userServiceClient.getById(userId);
        return eventMapper.toFullDto(event, initiator);
    }

    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventRequest dto) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (event.getState() == EventState.PUBLISHED) {
            throw new ForbiddenOperationException("Only pending or canceled events can be changed");
        }
        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }
        applyUpdate(event, dto);
        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == StateAction.SEND_TO_REVIEW) event.setState(EventState.PENDING);
            else if (dto.getStateAction() == StateAction.CANCEL_REVIEW) event.setState(EventState.CANCELED);
        }
        event = eventRepository.save(event);
        UserShortDto initiator = userServiceClient.getById(userId);
        return eventMapper.toFullDto(event, initiator);
    }

    public List<EventShortDto> getAllPublic(
        String text, List<Long> categories, Boolean paid,
        LocalDateTime rangeStart, LocalDateTime rangeEnd,
        Boolean onlyAvailable, String sort, Integer from, Integer size,
        HttpServletRequest request
    ) {
        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
        if (start != null && rangeEnd != null && start.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }
        final String searchText = (text != null && !text.isBlank()) ? text : null;
        final List<Long> catList = (categories != null && !categories.isEmpty()) ? categories : null;

        Sort pageSort = "RATING".equals(sort)
            ? Sort.by(Sort.Direction.DESC, "rating")
            : Sort.by(Sort.Direction.ASC, "eventDate");

        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));
            if (searchText != null) {
                String likeText = "%" + searchText.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("annotation")), likeText),
                    cb.like(cb.lower(root.get("description")), likeText)
                ));
            }
            if (catList != null) predicates.add(root.get("category").get("id").in(catList));
            if (paid != null) predicates.add(cb.equal(root.get("paid"), paid));
            if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), start));
            if (rangeEnd != null) predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            if (Boolean.TRUE.equals(onlyAvailable)) {
                predicates.add(cb.or(
                    cb.equal(root.get("participantLimit"), 0),
                    cb.lessThan(root.get("confirmedRequests"), root.get("participantLimit"))
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Event> events = eventRepository.findAll(spec, PageRequest.of(from / size, size, pageSort)).getContent();
        enrichWithRatings(events);
        Map<Long, UserShortDto> usersMap = fetchUsersMap(events);
        return events.stream().map(e -> eventMapper.toShortDto(e, usersMap.getOrDefault(e.getInitiatorId(),
            UserShortDto.builder().id(e.getInitiatorId()).name("").build()))).collect(Collectors.toList());
    }

    public EventFullDto getPublicById(Long id, Long userId) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        if (userId != null) {
            collectorClient.sendUserAction(userId, id, ActionTypeProto.ACTION_VIEW);
        }

        List<RecommendedEventProto> ratings = analyzerClient.getInteractionsCount(List.of(id));
        if (!ratings.isEmpty()) {
            event.setRating(ratings.get(0).getScore());
        }

        UserShortDto initiator = userServiceClient.getById(event.getInitiatorId());
        return eventMapper.toFullDto(event, initiator);
    }

    public List<EventShortDto> getRecommendations(Long userId) {
        List<RecommendedEventProto> recs = analyzerClient.getRecommendationsForUser(userId, DEFAULT_RECOMMENDATIONS_SIZE);
        if (recs.isEmpty()) return List.of();

        List<Long> eventIds = recs.stream().map(RecommendedEventProto::getEventId).collect(Collectors.toList());
        List<Event> events = eventRepository.findAllById(eventIds);

        Map<Long, Double> scoreMap = recs.stream()
            .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
        events.forEach(e -> e.setRating(scoreMap.getOrDefault(e.getId(), 0.0)));

        Map<Long, UserShortDto> usersMap = fetchUsersMap(events);
        return events.stream()
            .map(e -> eventMapper.toShortDto(e, usersMap.getOrDefault(e.getInitiatorId(),
                UserShortDto.builder().id(e.getInitiatorId()).name("").build())))
            .collect(Collectors.toList());
    }

    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        boolean hasVisited = requestServiceClient.getByEventId(eventId).stream()
            .anyMatch(r -> r.getRequester().equals(userId) && "CONFIRMED".equals(r.getStatus()));

        if (!hasVisited) {
            throw new BadRequestException("User with id=" + userId + " has not attended event with id=" + eventId);
        }

        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    public List<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return requestServiceClient.getByEventId(eventId);
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(
        Long userId, Long eventId, EventRequestStatusUpdateRequest dto
    ) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        try {
            RequestStatus.valueOf(dto.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + dto.getStatus());
        }

        if (RequestStatus.CONFIRMED.name().equals(dto.getStatus())
            && event.getParticipantLimit() != 0
            && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConditionsNotMetException("The participant limit has been reached");
        }

        EventRequestStatusUpdateResult result = requestServiceClient.updateStatuses(
            eventId, event.getParticipantLimit(), event.getConfirmedRequests(), dto
        );

        long newlyConfirmed = result.getConfirmedRequests() != null ? result.getConfirmedRequests().size() : 0;
        if (newlyConfirmed > 0) {
            event.setConfirmedRequests(event.getConfirmedRequests() + newlyConfirmed);
            eventRepository.save(event);
        }
        return result;
    }

    public EventForRequestDto getEventForRequest(Long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return EventForRequestDto.builder()
            .id(event.getId())
            .state(event.getState().name())
            .participantLimit(event.getParticipantLimit())
            .confirmedRequests(event.getConfirmedRequests())
            .initiatorId(event.getInitiatorId())
            .requestModeration(event.getRequestModeration())
            .build();
    }

    @Transactional
    public void updateConfirmedRequests(Long eventId, int delta) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        long updated = Math.max(0, event.getConfirmedRequests() + delta);
        event.setConfirmedRequests(updated);
        eventRepository.save(event);
    }

    private void applyUpdate(Event event, UpdateEventRequest dto) {
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) {
            event.setLat(dto.getLocation().getLat());
            event.setLon(dto.getLocation().getLon());
        }
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
    }

    private void enrichWithRatings(List<Event> events) {
        if (events.isEmpty()) return;
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        List<RecommendedEventProto> ratings = analyzerClient.getInteractionsCount(ids);
        if (!ratings.isEmpty()) {
            Map<Long, Double> ratingMap = ratings.stream()
                .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
            events.forEach(e -> e.setRating(ratingMap.getOrDefault(e.getId(), 0.0)));
        }
    }

    private Map<Long, UserShortDto> fetchUsersMap(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();
        List<Long> ids = events.stream().map(Event::getInitiatorId).distinct().collect(Collectors.toList());
        List<UserShortDto> users = userServiceClient.getByIds(ids);
        if (users == null || users.isEmpty()) return Collections.emptyMap();
        return users.stream().collect(Collectors.toMap(UserShortDto::getId, u -> u));
    }
}
