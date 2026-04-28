package com.example.eventservice.controller;

import com.example.eventservice.dto.CompilationDto;
import com.example.eventservice.dto.NewCompilationDto;
import com.example.eventservice.dto.UpdateCompilationRequest;
import com.example.eventservice.service.CompilationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CompilationController {

    private final CompilationService compilationService;

    @PostMapping("/admin/compilations")
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto create(@Valid @RequestBody NewCompilationDto dto) {
        return compilationService.create(dto);
    }

    @DeleteMapping("/admin/compilations/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long compId) {
        compilationService.delete(compId);
    }

    @PatchMapping("/admin/compilations/{compId}")
    public CompilationDto update(@PathVariable Long compId, @Valid @RequestBody UpdateCompilationRequest dto) {
        return compilationService.update(compId, dto);
    }

    @GetMapping("/compilations")
    public List<CompilationDto> getAll(
        @RequestParam(required = false) Boolean pinned,
        @RequestParam(defaultValue = "0") Integer from,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        return compilationService.getAll(pinned, from, size);
    }

    @GetMapping("/compilations/{compId}")
    public CompilationDto getById(@PathVariable Long compId) {
        return compilationService.getById(compId);
    }
}
