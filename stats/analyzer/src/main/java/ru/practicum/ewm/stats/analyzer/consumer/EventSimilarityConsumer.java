package ru.practicum.ewm.stats.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityConsumer {

    private final EventSimilarityRepository eventSimilarityRepository;

    @Transactional
    @KafkaListener(
        topics = "${kafka.topics.events-similarity:stats.events-similarity.v1}",
        containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    public void consume(EventSimilarityAvro similarity) {
        log.debug("Consumed similarity: eventA={}, eventB={}, score={}",
            similarity.getEventA(), similarity.getEventB(), similarity.getScore());
        eventSimilarityRepository.save(EventSimilarity.builder()
            .eventA(similarity.getEventA())
            .eventB(similarity.getEventB())
            .score(similarity.getScore())
            .timestamp(Instant.ofEpochMilli(similarity.getTimestamp()))
            .build());
    }
}
