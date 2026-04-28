package com.example.eventservice.client;

import com.example.eventservice.dto.EndpointHitDto;
import com.example.eventservice.dto.ViewStatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class StatsClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate rest;
    private final String serverUrl;

    public StatsClient(RestTemplate restTemplate,
                       @Value("${stats-server.url:http://stats-server}") String serverUrl) {
        this.rest = restTemplate;
        this.serverUrl = serverUrl;
    }

    public void recordHit(String uri, String ip) {
        try {
            EndpointHitDto hit = EndpointHitDto.builder()
                .app("event-service")
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
            rest.postForEntity(serverUrl + "/hit", hit, Void.class);
        } catch (Exception e) {
            log.error("Error recording hit to stats-server: {}", e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("unique", unique != null ? unique : false);
            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", uris.toArray());
            }
            URI requestUri = builder.build().encode().toUri();
            ResponseEntity<List<ViewStatsDto>> response = rest.exchange(
                requestUri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting stats from stats-server: {}", e.getMessage());
            return List.of();
        }
    }
}
