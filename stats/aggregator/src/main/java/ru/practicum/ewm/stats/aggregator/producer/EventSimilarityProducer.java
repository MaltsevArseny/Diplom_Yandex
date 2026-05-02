package ru.practicum.ewm.stats.aggregator.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityProducer {

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity:stats.events-similarity.v1}")
    private String topic;

    public void send(EventSimilarityAvro similarity) {
        String key = similarity.getEventA() + "-" + similarity.getEventB();
        kafkaTemplate.send(topic, key, similarity);
        log.debug("Sent similarity: eventA={}, eventB={}, score={}",
            similarity.getEventA(), similarity.getEventB(), similarity.getScore());
    }
}
