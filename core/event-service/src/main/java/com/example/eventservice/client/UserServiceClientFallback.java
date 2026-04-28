package com.example.eventservice.client;

import com.example.eventservice.dto.UserShortDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserShortDto getById(Long id) {
        return UserShortDto.builder().id(id).name("").build();
    }

    @Override
    public List<UserShortDto> getByIds(List<Long> ids) {
        return Collections.emptyList();
    }
}
