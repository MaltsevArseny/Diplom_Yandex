package com.example.mainservice.service;

import com.example.mainservice.dto.CategoryDto;
import com.example.mainservice.exception.ConditionsNotMetException;
import com.example.mainservice.exception.ConflictException;
import com.example.mainservice.exception.NotFoundException;
import com.example.mainservice.model.Category;
import com.example.mainservice.repository.CategoryRepository;
import com.example.mainservice.repository.EventRepository;
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
        Category category = Category.builder()
            .name(dto.getName())
            .build();
        category = categoryRepository.save(category);
        return toDto(category);
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
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public CategoryDto getById(Long catId) {
        Category category = categoryRepository.findById(catId)
            .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        return toDto(category);
    }

    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
            .id(category.getId())
            .name(category.getName())
            .build();
    }
}
