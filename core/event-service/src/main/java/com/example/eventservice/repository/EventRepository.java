package com.example.eventservice.repository;

import com.example.eventservice.model.Event;
import com.example.eventservice.model.EventState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    Optional<Event> findByIdAndState(Long id, EventState state);

    boolean existsByCategoryId(Long categoryId);
}
