package com.example.statsserver.service;

import com.example.statsserver.dto.EndpointHitDto;
import com.example.statsserver.dto.ViewStatsDto;
import com.example.statsserver.model.EndpointHit;
import com.example.statsserver.repository.HitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HitRepository hitRepository;

    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto dto) {
        EndpointHit hit = EndpointHit.builder()
            .app(dto.getApp())
            .uri(dto.getUri())
            .ip(dto.getIp())
            .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now())
            .build();
        hit = hitRepository.save(hit);
        return EndpointHitDto.builder()
            .id(hit.getId())
            .app(hit.getApp())
            .uri(hit.getUri())
            .ip(hit.getIp())
            .timestamp(hit.getTimestamp())
            .build();
    }

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        LocalDateTime startDt = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endDt = LocalDateTime.parse(end, FORMATTER);
        if (startDt.isAfter(endDt)) {
            throw new com.example.statsserver.exception.BadRequestException("start must be before end");
        }
        
        List<String> uriList = (uris == null || uris.isEmpty()) ? null : uris;
        if (Boolean.TRUE.equals(unique)) {
            if (uriList != null) {
                return hitRepository.findUniqueStatsWithUris(startDt, endDt, uriList);
            } else {
                return hitRepository.findUniqueStatsWithoutUris(startDt, endDt);
            }
        } else {
            if (uriList != null) {
                return hitRepository.findStatsWithUris(startDt, endDt, uriList);
            } else {
                return hitRepository.findStatsWithoutUris(startDt, endDt);
            }
        }
    }
}
