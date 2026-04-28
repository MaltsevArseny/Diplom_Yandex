package com.example.eventservice.service;

import com.example.eventservice.dto.CategoryDto;
import com.example.eventservice.exception.ConditionsNotMetException;
import com.example.eventservice.exception.ConflictException;
import com.example.eventservice.exception.NotFoundException;
import com.example.eventservice.model.Category;
import com.example.eventservice.repository.CategoryRepository;
import com.example.eventservice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CategoryDto create(CategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name " + dto.getName() + " already exists");
        }
        Category category = Category.builder().name(dto.getName()).build();
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long catId) {
        Category category = categoryRepository.findById(catId)
            .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConditionsNotMetException("The category is not empty");
        }
        categoryRepository.delete(category);
    }

    @Transactional
    public CategoryDto update(Long catId, CategoryDto dto) {
        Category category = categoryRepository.findById(catId)
            .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        if (!category.getName().equals(dto.getName()) && categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name " + dto.getName() + " already exists");
        }
        category.setName(dto.getName());
        return toDto(categoryRepository.save(category));
    }

    public List<CategoryDto> getAll(Integer from, Integer size) {
        return categoryRepository.findAll(PageRequest.of(from / size, size))
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public CategoryDto getById(Long catId) {
        return toDto(categoryRepository.findById(catId)
            .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found")));
    }

    private CategoryDto toDto(Category c) {
        return CategoryDto.builder().id(c.getId()).name(c.getName()).build();
    }
}
