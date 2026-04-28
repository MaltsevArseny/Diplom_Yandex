package com.example.userservice.controller;

import com.example.userservice.dto.UserShortDto;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserShortDto getById(@PathVariable Long id) {
        return userService.getShortById(id);
    }

    @GetMapping
    public List<UserShortDto> getByIds(@RequestParam List<Long> ids) {
        return userService.getShortByIds(ids);
    }

    @GetMapping("/{id}/exists")
    public Boolean exists(@PathVariable Long id) {
        return userService.exists(id);
    }
}
