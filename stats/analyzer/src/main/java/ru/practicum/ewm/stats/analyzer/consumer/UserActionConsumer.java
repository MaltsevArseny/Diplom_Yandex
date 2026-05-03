package ru.practicum.ewm.stats.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.model.UserAction;
import ru.practicum.ewm.stats.analyzer.model.UserActionId;
import ru.practicum.ewm.stats.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private static final Map<ActionTypeAvro, Double> ACTION_WEIGHTS = Map.of(
        ActionTypeAvro.VIEW, 0.4,
        ActionTypeAvro.REGISTER, 0.8,
        ActionTypeAvro.LIKE, 1.0
    );

    private final UserActionRepository userActionRepository;

    @Transactional
    @KafkaListener(
        topics = "${kafka.topics.user-actions:stats.user-actions.v1}",
        containerFactory = "userActionKafkaListenerContainerFactory"
    )
    public void consume(UserActionAvro action) {
        log.debug("Consumed user action: userId={}, eventId={}", action.getUserId(), action.getEventId());
        double newWeight = ACTION_WEIGHTS.getOrDefault(action.getActionType(), 0.4);

        Optional<UserAction> existing = userActionRepository
            .findById(new UserActionId(action.getUserId(), action.getEventId()));

        if (existing.isEmpty() || existing.get().getWeight() < newWeight) {
            userActionRepository.save(UserAction.builder()
                .userId(action.getUserId())
                .eventId(action.getEventId())
                .weight(newWeight)
                .timestamp(Instant.ofEpochMilli(action.getTimestamp()))
                .build());
        }
    }
}
