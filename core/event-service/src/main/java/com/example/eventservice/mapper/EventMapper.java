package com.example.eventservice.mapper;

import com.example.eventservice.dto.CategoryDto;
import com.example.eventservice.dto.EventFullDto;
import com.example.eventservice.dto.EventShortDto;
import com.example.eventservice.dto.LocationDto;
import com.example.eventservice.dto.UserShortDto;
import com.example.eventservice.model.Category;
import com.example.eventservice.model.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder().id(category.getId()).name(category.getName()).build();
    }

    public EventShortDto toShortDto(Event event, UserShortDto initiator) {
        return EventShortDto.builder()
            .id(event.getId())
            .annotation(event.getAnnotation())
            .category(toCategoryDto(event.getCategory()))
            .confirmedRequests(event.getConfirmedRequests())
            .eventDate(event.getEventDate())
            .initiator(initiator)
            .paid(event.getPaid())
            .title(event.getTitle())
            .rating(event.getRating())
            .build();
    }

    public EventFullDto toFullDto(Event event, UserShortDto initiator) {
        LocationDto location = null;
        if (event.getLat() != null && event.getLon() != null) {
            location = LocationDto.builder().lat(event.getLat()).lon(event.getLon()).build();
        }
        return EventFullDto.builder()
            .id(event.getId())
            .annotation(event.getAnnotation())
            .category(toCategoryDto(event.getCategory()))
            .confirmedRequests(event.getConfirmedRequests())
            .createdOn(event.getCreatedOn())
            .description(event.getDescription())
            .eventDate(event.getEventDate())
            .initiator(initiator)
            .location(location)
            .paid(event.getPaid())
            .participantLimit(event.getParticipantLimit())
            .publishedOn(event.getPublishedOn())
            .requestModeration(event.getRequestModeration())
            .state(event.getState().name())
            .title(event.getTitle())
            .rating(event.getRating())
            .build();
    }
}
