package com.example.requestservice.repository;

import com.example.requestservice.model.ParticipationRequest;
import com.example.requestservice.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByIdIn(List<Long> ids);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long id, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT r.eventId, COUNT(r) FROM ParticipationRequest r WHERE r.eventId IN :eventIds AND r.status = :status GROUP BY r.eventId")
    List<Object[]> countByEventIds(@Param("eventIds") List<Long> eventIds, @Param("status") RequestStatus status);
}
