package com.example.statsserver.repository;

import com.example.statsserver.dto.ViewStatsDto;
import com.example.statsserver.model.EndpointHit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HitRepository extends JpaRepository<EndpointHit, Long> {

    @Query("SELECT new com.example.statsserver.dto.ViewStatsDto(h.app, h.uri, COUNT(h.ip)) "
        + "FROM EndpointHit h "
        + "WHERE h.timestamp BETWEEN :start AND :end "
        + "AND h.uri IN :uris "
        + "GROUP BY h.app, h.uri "
        + "ORDER BY COUNT(h.ip) DESC")
    List<ViewStatsDto> findStatsWithUris(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("uris") List<String> uris
    );

    @Query("SELECT new com.example.statsserver.dto.ViewStatsDto(h.app, h.uri, COUNT(h.ip)) "
        + "FROM EndpointHit h "
        + "WHERE h.timestamp BETWEEN :start AND :end "
        + "GROUP BY h.app, h.uri "
        + "ORDER BY COUNT(h.ip) DESC")
    List<ViewStatsDto> findStatsWithoutUris(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT new com.example.statsserver.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip)) "
        + "FROM EndpointHit h "
        + "WHERE h.timestamp BETWEEN :start AND :end "
        + "AND h.uri IN :uris "
        + "GROUP BY h.app, h.uri "
        + "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStatsDto> findUniqueStatsWithUris(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("uris") List<String> uris
    );

    @Query("SELECT new com.example.statsserver.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip)) "
        + "FROM EndpointHit h "
        + "WHERE h.timestamp BETWEEN :start AND :end "
        + "GROUP BY h.app, h.uri "
        + "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStatsDto> findUniqueStatsWithoutUris(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
