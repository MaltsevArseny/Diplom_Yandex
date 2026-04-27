package com.example.mainservice.client;

import com.example.mainservice.dto.EndpointHitDto;
import com.example.mainservice.dto.ViewStatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
public class StatsClient {

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
                .app("main-service")
                .uri(uri)
                .ip(ip)
                .timestamp(java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
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
