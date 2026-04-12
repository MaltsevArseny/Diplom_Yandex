package com.example.mainservice.service;

import com.example.mainservice.dto.CompilationDto;
import com.example.mainservice.dto.NewCompilationDto;
import com.example.mainservice.dto.UpdateCompilationRequest;
import com.example.mainservice.exception.NotFoundException;
import com.example.mainservice.model.Compilation;
import com.example.mainservice.model.Event;
import com.example.mainservice.repository.CompilationRepository;
import com.example.mainservice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationService {

    private final CompilationRepository compilationRepository;

    private final EventRepository eventRepository;

    private final EventService eventService;

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
        Compilation compilation = compilationRepository.findById(compId)
            .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        if (dto.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            compilation.setEvents(events);
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
        return toDto(compilationRepository.save(compilation));
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size) {
        PageRequest pageable = PageRequest.of(from / size, size);
        if (pinned != null) {
            return compilationRepository.findAllByPinned(pinned, pageable).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        }
        return compilationRepository.findAll(pageable).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompilationDto getById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
            .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        return toDto(compilation);
    }

    @Transactional(readOnly = true)
    private CompilationDto toDto(Compilation compilation) {
        Set<com.example.mainservice.dto.EventShortDto> eventShorts =
            compilation.getEvents() == null ? Collections.emptySet() :
            compilation.getEvents().stream()
                .map(eventService::toShortDto)
                .collect(Collectors.toSet());
        return CompilationDto.builder()
            .id(compilation.getId())
            .events(eventShorts)
            .pinned(compilation.getPinned())
            .title(compilation.getTitle())
            .build();
    }
}
