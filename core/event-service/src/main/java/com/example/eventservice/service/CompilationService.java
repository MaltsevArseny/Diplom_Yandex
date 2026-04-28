package com.example.eventservice.service;

import com.example.eventservice.client.UserServiceClient;
import com.example.eventservice.dto.CompilationDto;
import com.example.eventservice.dto.EventShortDto;
import com.example.eventservice.dto.NewCompilationDto;
import com.example.eventservice.dto.UpdateCompilationRequest;
import com.example.eventservice.dto.UserShortDto;
import com.example.eventservice.exception.BadRequestException;
import com.example.eventservice.exception.NotFoundException;
import com.example.eventservice.mapper.EventMapper;
import com.example.eventservice.model.Compilation;
import com.example.eventservice.model.Event;
import com.example.eventservice.repository.CompilationRepository;
import com.example.eventservice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserServiceClient userServiceClient;

    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        Set<Event> events = new HashSet<>();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
        }
        Compilation compilation = Compilation.builder()
            .events(events)
            .pinned(dto.getPinned() != null ? dto.getPinned() : false)
            .title(dto.getTitle())
            .build();
        return toDto(compilationRepository.save(compilation));
    }

    @Transactional
    public void delete(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        if (dto.getTitle() != null && (dto.getTitle().isBlank() || dto.getTitle().length() > 50)) {
            throw new BadRequestException("Title must be between 1 and 50 characters");
        }
        Compilation compilation = compilationRepository.findById(compId)
            .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        if (dto.getEvents() != null) {
            compilation.setEvents(new HashSet<>(eventRepository.findAllById(dto.getEvents())));
        }
        if (dto.getPinned() != null) compilation.setPinned(dto.getPinned());
        if (dto.getTitle() != null) compilation.setTitle(dto.getTitle());
        return toDto(compilationRepository.save(compilation));
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size) {
        PageRequest pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations = pinned != null
            ? compilationRepository.findAllByPinned(pinned, pageable).getContent()
            : compilationRepository.findAll(pageable).getContent();
        return compilations.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompilationDto getById(Long compId) {
        return toDto(compilationRepository.findById(compId)
            .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found")));
    }

    private CompilationDto toDto(Compilation compilation) {
        Set<Event> events = compilation.getEvents();
        Map<Long, UserShortDto> usersMap = Collections.emptyMap();
        if (events != null && !events.isEmpty()) {
            List<Long> ids = events.stream().map(Event::getInitiatorId).distinct().collect(Collectors.toList());
            List<UserShortDto> users = userServiceClient.getByIds(ids);
            if (users != null) {
                usersMap = users.stream().collect(Collectors.toMap(UserShortDto::getId, u -> u));
            }
        }
        final Map<Long, UserShortDto> finalUsersMap = usersMap;
        Set<EventShortDto> eventShorts = events == null ? Collections.emptySet() : events.stream()
            .map(e -> eventMapper.toShortDto(e, finalUsersMap.getOrDefault(e.getInitiatorId(),
                UserShortDto.builder().id(e.getInitiatorId()).name("").build())))
            .collect(Collectors.toSet());
        return CompilationDto.builder()
            .id(compilation.getId())
            .events(eventShorts)
            .pinned(compilation.getPinned())
            .title(compilation.getTitle())
            .build();
    }
}
