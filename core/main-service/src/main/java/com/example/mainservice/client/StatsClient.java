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

import java.util.List;

@Slf4j
@Service
public class StatsClient {

    private final RestTemplate rest;
    private final String serverUrl;

    public StatsClient(@Value("${stats-server.url:http://stats-server:9090}") String serverUrl) {
        this.rest = new RestTemplate();
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
            java.util.Map<String, Object> parameters = new java.util.HashMap<>();
            parameters.put("start", start);
            parameters.put("end", end);

            StringBuilder url = new StringBuilder(serverUrl + "/stats?start={start}&end={end}");
            if (uris != null && !uris.isEmpty()) {
                url.append("&uris={uris}");
                parameters.put("uris", String.join(",", uris));
            }
            if (unique != null) {
                url.append("&unique={unique}");
                parameters.put("unique", unique);
            }

            ResponseEntity<List<ViewStatsDto>> response = rest.exchange(
                url.toString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {},
                parameters
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting stats from stats-server: {}", e.getMessage());
            return List.of();
        }
    }
}
