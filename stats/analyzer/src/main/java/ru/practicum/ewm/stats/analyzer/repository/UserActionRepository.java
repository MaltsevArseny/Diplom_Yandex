package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.model.UserAction;
import ru.practicum.ewm.stats.analyzer.model.UserActionId;

import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, UserActionId> {

    List<UserAction> findAllByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    List<UserAction> findAllByUserId(Long userId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ua FROM UserAction ua WHERE ua.eventId IN :eventIds AND ua.userId = :userId")
    List<UserAction> findByUserIdAndEventIdIn(@Param("userId") Long userId,
                                              @Param("eventIds") List<Long> eventIds);

    @Query("SELECT ua.eventId, SUM(ua.weight) FROM UserAction ua WHERE ua.eventId IN :eventIds GROUP BY ua.eventId")
    List<Object[]> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);
}
