package com.example.mainservice.repository;

import com.example.mainservice.model.Event;
import com.example.mainservice.model.EventState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Optional<Event> findByIdAndState(Long id, EventState state);

    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT e FROM Event e WHERE "
        + "(:users IS NULL OR e.initiator.id IN :users) AND "
        + "(:states IS NULL OR e.state IN :states) AND "
        + "(:categories IS NULL OR e.category.id IN :categories) AND "
        + "(:rangeStart IS NULL OR e.eventDate >= :rangeStart) AND "
        + "(:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findAllByAdmin(
        @Param("users") List<Long> users,
        @Param("states") List<EventState> states,
        @Param("categories") List<Long> categories,
        @Param("rangeStart") LocalDateTime rangeStart,
        @Param("rangeEnd") LocalDateTime rangeEnd,
        Pageable pageable
    );

    @Query("SELECT e FROM Event e WHERE "
        + "e.state = 'PUBLISHED' AND "
        + "(:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) "
        + "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) AND "
        + "(:categories IS NULL OR e.category.id IN :categories) AND "
        + "(:paid IS NULL OR e.paid = :paid) AND "
        + "(:rangeStart IS NULL OR e.eventDate >= :rangeStart) AND "
        + "(:rangeEnd IS NULL OR e.eventDate <= :rangeEnd) AND "
        + "(:onlyAvailable = FALSE OR (e.participantLimit = 0 OR e.confirmedRequests < e.participantLimit))")
    Page<Event> findAllPublic(
        @Param("text") String text,
        @Param("categories") List<Long> categories,
        @Param("paid") Boolean paid,
        @Param("rangeStart") LocalDateTime rangeStart,
        @Param("rangeEnd") LocalDateTime rangeEnd,
        @Param("onlyAvailable") Boolean onlyAvailable,
        Pageable pageable
    );
}
