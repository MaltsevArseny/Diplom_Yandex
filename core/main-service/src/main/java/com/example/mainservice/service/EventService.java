package com.example.mainservice.service;

import com.example.mainservice.client.StatsClient;
import com.example.mainservice.dto.EventFullDto;
import com.example.mainservice.dto.EventRequestStatusUpdateRequest;
import com.example.mainservice.dto.EventRequestStatusUpdateResult;
import com.example.mainservice.dto.EventShortDto;
import com.example.mainservice.dto.NewEventDto;
import com.example.mainservice.dto.ParticipationRequestDto;
import com.example.mainservice.dto.UpdateEventRequest;
import com.example.mainservice.dto.ViewStatsDto;
import com.example.mainservice.exception.BadRequestException;
import com.example.mainservice.exception.ConditionsNotMetException;
import com.example.mainservice.exception.ConflictException;
import com.example.mainservice.exception.ForbiddenOperationException;
import com.example.mainservice.exception.NotFoundException;
import com.example.mainservice.mapper.EventMapper;
import com.example.mainservice.model.Category;
import com.example.mainservice.model.Event;
import com.example.mainservice.model.EventState;
import com.example.mainservice.model.ParticipationRequest;
import com.example.mainservice.model.RequestStatus;
import com.example.mainservice.model.StateAction;
import com.example.mainservice.model.User;
import com.example.mainservice.repository.CategoryRepository;
import com.example.mainservice.repository.EventRepository;
import com.example.mainservice.repository.RequestRepository;
import com.example.mainservice.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final CategoryRepository categoryRepository;

    private final RequestRepository requestRepository;

    private final StatsClient statsClient;

    private final EventMapper eventMapper;

    public List<EventFullDto> getAllByAdmin(
        List<Long> users,
        List<String> states,
        List<Long> categories,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        Integer from,
        Integer size
    ) {
        List<EventState> statesList = null;
        if (states != null) {
            try {
                statesList = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid event state");
            }
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }
        List<Long> catList = (categories != null && !categories.isEmpty()) ? categories : null;
        final List<EventState> finalStatesList = statesList;

        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (users != null && !users.isEmpty()) {
                predicates.add(root.get("initiator").get("id").in(users));
            }
            if (finalStatesList != null && !finalStatesList.isEmpty()) {
                predicates.add(root.get("state").in(finalStatesList));
            }
            if (catList != null && !catList.isEmpty()) {
                predicates.add(root.get("category").get("id").in(catList));
            }
            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Event> events = eventRepository.findAll(spec, PageRequest.of(from / size, size)).getContent();

        if (!events.isEmpty()) {
            List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());
            List<ViewStatsDto> stats = statsClient.getStats(
                LocalDateTime.now().minusYears(10).format(FORMATTER),
                LocalDateTime.now().plusYears(10).format(FORMATTER),
                uris,
                true
            );
            if (stats != null && !stats.isEmpty()) {
                Map<String, Long> viewsMap = stats.stream()
                    .collect(Collectors.toMap(
                        ViewStatsDto::getUri,
                        ViewStatsDto::getHits,
                        (h1, h2) -> h1 + h2
                    ));
                events.forEach(e -> e.setViews(viewsMap.getOrDefault("/events/" + e.getId(), 0L)));
            }
        }

        return events.stream().map(eventMapper::toFullDto).collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventRequest dto) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        applyUpdate(event, dto);
        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ForbiddenOperationException("Cannot publish the event because it's not in the right state: "
                        + event.getState());
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
        return eventMapper.toFullDto(eventRepository.save(event));
    }

    public List<EventShortDto> getByUserId(Long userId, Integer from, Integer size) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        return eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size))
            .stream().map(eventMapper::toShortDto).collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
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
            .initiator(user)
            .lat(dto.getLocation() != null ? dto.getLocation().getLat() : null)
            .lon(dto.getLocation() != null ? dto.getLocation().getLon() : null)
            .paid(dto.getPaid() != null ? dto.getPaid() : false)
            .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
            .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
            .state(EventState.PENDING)
            .title(dto.getTitle())
            .views(0L)
            .build();
        return eventMapper.toFullDto(eventRepository.save(event));
    }

    public EventFullDto getByIdAndUser(Long userId, Long eventId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return eventMapper.toFullDto(event);
    }

    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventRequest dto) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
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
            if (dto.getStateAction() == StateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (dto.getStateAction() == StateAction.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }
        return eventMapper.toFullDto(eventRepository.save(event));
    }

    public List<EventShortDto> getAllPublic(
        String text,
        List<Long> categories,
        Boolean paid,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        Boolean onlyAvailable,
        String sort,
        Integer from,
        Integer size,
        HttpServletRequest request
    ) {
        String searchText = (text != null && !text.isBlank()) ? text : null;
        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime end = rangeEnd;
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }
        List<Long> catList = (categories != null && !categories.isEmpty()) ? categories : null;
        statsClient.recordHit(request.getRequestURI(), request.getRemoteAddr());
        Sort pageSort = "VIEWS".equals(sort)
            ? Sort.by(Sort.Direction.DESC, "views")
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
            if (catList != null && !catList.isEmpty()) {
                predicates.add(root.get("category").get("id").in(catList));
            }
            if (paid != null) {
                predicates.add(cb.equal(root.get("paid"), paid));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), end));
            }
            if (onlyAvailable != null && onlyAvailable) {
                predicates.add(cb.or(
                    cb.equal(root.get("participantLimit"), 0),
                    cb.lessThan(root.get("confirmedRequests"), root.get("participantLimit"))
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Event> events = eventRepository.findAll(spec, PageRequest.of(from / size, size, pageSort)).getContent();

        if (!events.isEmpty()) {
            List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());
            List<ViewStatsDto> stats = statsClient.getStats(
                start.minusYears(1).format(FORMATTER),
                LocalDateTime.now().plusYears(10).format(FORMATTER),
                uris,
                true
            );
            if (stats != null && !stats.isEmpty()) {
                Map<String, Long> viewsMap = stats.stream()
                    .collect(Collectors.toMap(
                        ViewStatsDto::getUri,
                        ViewStatsDto::getHits,
                        (h1, h2) -> h1 + h2
                    ));
                events.forEach(e -> e.setViews(viewsMap.getOrDefault("/events/" + e.getId(), 0L)));
            }
        }

        return events.stream().map(eventMapper::toShortDto).collect(Collectors.toList());
    }

    public EventFullDto getPublicById(Long id, HttpServletRequest request) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));
        statsClient.recordHit(request.getRequestURI(), request.getRemoteAddr());
        List<ViewStatsDto> stats = statsClient.getStats(
            LocalDateTime.now().minusYears(10).format(FORMATTER),
            LocalDateTime.now().plusYears(10).format(FORMATTER),
            List.of(request.getRequestURI()),
            true
        );
        if (stats != null && !stats.isEmpty()) {
            event.setViews(stats.get(0).getHits());
        }
        return eventMapper.toFullDto(event);
    }

    public List<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return requestRepository.findAllByEventId(eventId).stream()
            .map(eventMapper::toRequestDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(
        Long userId,
        Long eventId,
        EventRequestStatusUpdateRequest updateRequest
    ) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (updateRequest.getStatus() == RequestStatus.CONFIRMED
            && event.getParticipantLimit() != 0
            && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConditionsNotMetException("The participant limit has been reached");
        }
        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        for (ParticipationRequest req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (event.getParticipantLimit() != 0
                    && event.getConfirmedRequests() >= event.getParticipantLimit()) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(eventMapper.toRequestDto(req));
                } else {
                    req.setStatus(RequestStatus.CONFIRMED);
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                    confirmed.add(eventMapper.toRequestDto(req));
                }
            } else {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(eventMapper.toRequestDto(req));
            }
            requestRepository.save(req);
        }
        eventRepository.save(event);
        return EventRequestStatusUpdateResult.builder()
            .confirmedRequests(confirmed)
            .rejectedRequests(rejected)
            .build();
    }

    private void applyUpdate(Event event, UpdateEventRequest dto) {
        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getLocation() != null) {
            event.setLat(dto.getLocation().getLat());
            event.setLon(dto.getLocation().getLon());
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
    }
}
