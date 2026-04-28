package com.example.eventservice.client;

import com.example.eventservice.dto.UserShortDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/internal/users/{id}")
    UserShortDto getById(@PathVariable Long id);

    @GetMapping("/internal/users")
    List<UserShortDto> getByIds(@RequestParam List<Long> ids);
}
