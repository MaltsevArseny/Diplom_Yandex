package com.example.eventservice.controller;

import com.example.eventservice.dto.CategoryDto;
import com.example.eventservice.service.CategoryService;
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
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@Valid @RequestBody CategoryDto dto) {
        return categoryService.create(dto);
    }

    @DeleteMapping("/admin/categories/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long catId) {
        categoryService.delete(catId);
    }

    @PatchMapping("/admin/categories/{catId}")
    public CategoryDto update(@PathVariable Long catId, @Valid @RequestBody CategoryDto dto) {
        return categoryService.update(catId, dto);
    }

    @GetMapping("/categories")
    public List<CategoryDto> getAll(
        @RequestParam(defaultValue = "0") Integer from,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        return categoryService.getAll(from, size);
    }

    @GetMapping("/categories/{catId}")
    public CategoryDto getById(@PathVariable Long catId) {
        return categoryService.getById(catId);
    }
}
