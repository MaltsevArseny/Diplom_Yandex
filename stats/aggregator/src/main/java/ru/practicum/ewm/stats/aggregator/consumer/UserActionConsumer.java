package ru.practicum.ewm.stats.aggregator.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.producer.EventSimilarityProducer;
import ru.practicum.ewm.stats.aggregator.service.EventSimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final EventSimilarityService similarityService;
    private final EventSimilarityProducer similarityProducer;

    @KafkaListener(
        topics = "${kafka.topics.user-actions:stats.user-actions.v1}",
        groupId = "${spring.kafka.consumer.group-id:aggregator-group}"
    )
    public void consume(UserActionAvro action) {
        log.debug("Consumed user action: userId={}, eventId={}", action.getUserId(), action.getEventId());
        List<EventSimilarityAvro> similarities = similarityService.processAction(action);
        similarities.forEach(similarityProducer::send);
    }
}
