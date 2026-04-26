package com.example.mainservice.mapper;

import com.example.mainservice.dto.CategoryDto;
import com.example.mainservice.dto.EventFullDto;
import com.example.mainservice.dto.EventShortDto;
import com.example.mainservice.dto.LocationDto;
import com.example.mainservice.dto.ParticipationRequestDto;
import com.example.mainservice.dto.UserShortDto;
import com.example.mainservice.model.Category;
import com.example.mainservice.model.Event;
import com.example.mainservice.model.ParticipationRequest;
import com.example.mainservice.model.User;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder()
            .id(category.getId())
            .name(category.getName())
            .build();
    }

    public UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
            .id(user.getId())
            .name(user.getName())
            .build();
    }

    public EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
            .id(event.getId())
            .annotation(event.getAnnotation())
            .category(toCategoryDto(event.getCategory()))
            .confirmedRequests(event.getConfirmedRequests())
            .eventDate(event.getEventDate())
            .initiator(toUserShortDto(event.getInitiator()))
            .paid(event.getPaid())
            .title(event.getTitle())
            .views(event.getViews())
            .build();
    }

    public EventFullDto toFullDto(Event event) {
        LocationDto location = null;
        if (event.getLat() != null && event.getLon() != null) {
            location = LocationDto.builder()
                .lat(event.getLat())
                .lon(event.getLon())
                .build();
        }
        return EventFullDto.builder()
            .id(event.getId())
            .annotation(event.getAnnotation())
            .category(toCategoryDto(event.getCategory()))
            .confirmedRequests(event.getConfirmedRequests())
            .createdOn(event.getCreatedOn())
            .description(event.getDescription())
            .eventDate(event.getEventDate())
            .initiator(toUserShortDto(event.getInitiator()))
            .location(location)
            .paid(event.getPaid())
            .participantLimit(event.getParticipantLimit())
            .publishedOn(event.getPublishedOn())
            .requestModeration(event.getRequestModeration())
            .state(event.getState().name())
            .title(event.getTitle())
            .views(event.getViews())
            .build();
    }

    public ParticipationRequestDto toRequestDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
            .id(request.getId())
            .created(request.getCreated())
            .event(request.getEvent().getId())
            .requester(request.getRequester().getId())
            .status(request.getStatus().name())
            .build();
    }
}