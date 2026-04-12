package com.example.mainservice.service;

import com.example.mainservice.client.StatsClient;
import com.example.mainservice.dto.CategoryDto;
import com.example.mainservice.dto.EventFullDto;
import com.example.mainservice.dto.EventRequestStatusUpdateRequest;
import com.example.mainservice.dto.EventRequestStatusUpdateResult;
import com.example.mainservice.dto.EventShortDto;
import com.example.mainservice.dto.LocationDto;
import com.example.mainservice.dto.NewEventDto;
import com.example.mainservice.dto.ParticipationRequestDto;
import com.example.mainservice.dto.UpdateEventRequest;
import com.example.mainservice.dto.UserShortDto;
import com.example.mainservice.dto.ViewStatsDto;
import com.example.mainservice.exception.BadRequestException;
import com.example.mainservice.exception.ConflictException;
import com.example.mainservice.exception.NotFoundException;
import com.example.mainservice.model.Category;
import com.example.mainservice.model.Event;
import com.example.mainservice.model.EventState;
import com.example.mainservice.model.ParticipationRequest;
import com.example.mainservice.model.RequestStatus;
import com.example.mainservice.model.User;
import com.example.mainservice.repository.CategoryRepository;
import com.example.mainservice.repository.EventRepository;
import com.example.mainservice.repository.RequestRepository;
import com.example.mainservice.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    public List<EventFullDto> getAllByAdmin(
        List<Long> users,
        List<String> states,
        List<Long> categories,
        String rangeStart,
        String rangeEnd,
        Integer from,
        Integer size
    ) {
        List<EventState> stateList = null;
        if (states != null && !states.isEmpty()) {
            stateList = states.stream().map(EventState::valueOf).collect(Collectors.toList());
        }
        List<Long> userList = (users != null && !users.isEmpty()) ? users : null;
        List<Long> catList = (categories != null && !categories.isEmpty()) ? categories : null;
        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }
        return eventRepository.findAllByAdmin(userList, stateList, catList, start, end,
            PageRequest.of(from / size, size)).stream()
            .map(this::toFullDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventRequest dto) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        applyUpdate(event, dto);
        if (dto.getStateAction() != null) {
            if ("PUBLISH_EVENT".equals(dto.getStateAction())) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in the right state: "
                        + event.getState());
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if ("REJECT_EVENT".equals(dto.getStateAction())) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject the event because it's already published");
                }
                event.setState(EventState.CANCELED);
            }
        }
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Event date must be at least 1 hour from now for admin publication");
        }
        return toFullDto(eventRepository.save(event));
    }

    public List<EventShortDto> getByUserId(Long userId, Integer from, Integer size) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        return eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size))
            .stream().map(this::toShortDto).collect(Collectors.toList());
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
        return toFullDto(eventRepository.save(event));
    }

    public EventFullDto getByIdAndUser(Long userId, Long eventId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return toFullDto(event);
    }

    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventRequest dto) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }
        applyUpdate(event, dto);
        if (dto.getStateAction() != null) {
            if ("SEND_TO_REVIEW".equals(dto.getStateAction())) {
                event.setState(EventState.PENDING);
            } else if ("CANCEL_REVIEW".equals(dto.getStateAction())) {
                event.setState(EventState.CANCELED);
            }
        }
        return toFullDto(eventRepository.save(event));
    }

    public List<EventShortDto> getAllPublic(
        String text,
        List<Long> categories,
        Boolean paid,
        String rangeStart,
        String rangeEnd,
        Boolean onlyAvailable,
        String sort,
        Integer from,
        Integer size,
        HttpServletRequest request
    ) {
        String searchText = (text != null && !text.isBlank()) ? text : null;
        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }
        List<Long> catList = (categories != null && !categories.isEmpty()) ? categories : null;
        statsClient.recordHit(request.getRequestURI(), request.getRemoteAddr());
        Sort pageSort = "VIEWS".equals(sort)
            ? Sort.by(Sort.Direction.DESC, "views")
            : Sort.by(Sort.Direction.ASC, "eventDate");
        List<Event> events = eventRepository.findAllPublic(searchText, catList, paid, start, end,
            onlyAvailable != null ? onlyAvailable : false,
            PageRequest.of(from / size, size, pageSort)).getContent();

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

        return events.stream().map(this::toShortDto).collect(Collectors.toList());
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
        return toFullDto(event);
    }

    public List<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        eventRepository.findByIdAndInitiatorId(eventId, userId)
            .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return requestRepository.findAllByEventId(eventId).stream()
            .map(this::toRequestDto)
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
        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
            if ("CONFIRMED".equals(updateRequest.getStatus())) {
                if (event.getParticipantLimit() != 0
                    && event.getConfirmedRequests() >= event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(toRequestDto(request));
                } else {
                    request.setStatus(RequestStatus.CONFIRMED);
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                    confirmed.add(toRequestDto(request));
                }
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(toRequestDto(request));
            }
            requestRepository.save(request);
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

    private CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder()
            .id(category.getId())
            .name(category.getName())
            .build();
    }

    private UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
            .id(user.getId())
            .name(user.getName())
            .build();
    }

    public EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
            .id(event.getId())
            .annotation(event.getAnnotation())
            .category(toCategoryDto(event.getCategory()))
            .confirmedRequests(event.getConfirmedRequests())
            .eventDate(event.getEventDate())
            .initiator(toUserShortDto(event.getInitiator()))
            .paid(event.getPaid())
            .title(event.getTitle())
            .views(event.getViews())
            .build();
    }

    public EventFullDto toFullDto(Event event) {
        LocationDto location = null;
        if (event.getLat() != null && event.getLon() != null) {
            location = LocationDto.builder()
                .lat(event.getLat())
                .lon(event.getLon())
                .build();
        }
        return EventFullDto.builder()
            .id(event.getId())
            .annotation(event.getAnnotation())
            .category(toCategoryDto(event.getCategory()))
            .confirmedRequests(event.getConfirmedRequests())
            .createdOn(event.getCreatedOn())
            .description(event.getDescription())
            .eventDate(event.getEventDate())
            .initiator(toUserShortDto(event.getInitiator()))
            .location(location)
            .paid(event.getPaid())
            .participantLimit(event.getParticipantLimit())
            .publishedOn(event.getPublishedOn())
            .requestModeration(event.getRequestModeration())
            .state(event.getState().name())
            .title(event.getTitle())
            .views(event.getViews())
            .build();
    }

    private ParticipationRequestDto toRequestDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
            .id(request.getId())
            .created(request.getCreated())
            .event(request.getEvent().getId())
            .requester(request.getRequester().getId())
            .status(request.getStatus().name())
            .build();
    }
}
