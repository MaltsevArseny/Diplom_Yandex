package com.example.userservice.service;

import com.example.userservice.dto.UserDto;
import com.example.userservice.dto.UserShortDto;
import com.example.userservice.exception.ConflictException;
import com.example.userservice.exception.NotFoundException;
import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto create(UserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("User with email " + dto.getEmail() + " already exists");
        }
        User user = User.builder().name(dto.getName()).email(dto.getEmail()).build();
        user = userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public void delete(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
    }

    public List<UserDto> getAll(List<Long> ids, Integer from, Integer size) {
        PageRequest pageable = PageRequest.of(from / size, size);
        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(pageable).stream().map(this::toDto).collect(Collectors.toList());
        }
        return userRepository.findAllByIdIn(ids, pageable).stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserShortDto getShortById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        return UserShortDto.builder().id(user.getId()).name(user.getName()).build();
    }

    public List<UserShortDto> getShortByIds(List<Long> ids) {
        return userRepository.findAllByIdIn(ids).stream()
            .map(u -> UserShortDto.builder().id(u.getId()).name(u.getName()).build())
            .collect(Collectors.toList());
    }

    public boolean exists(Long userId) {
        return userRepository.existsById(userId);
    }

    private UserDto toDto(User user) {
        return UserDto.builder().id(user.getId()).name(user.getName()).email(user.getEmail()).build();
    }
}
