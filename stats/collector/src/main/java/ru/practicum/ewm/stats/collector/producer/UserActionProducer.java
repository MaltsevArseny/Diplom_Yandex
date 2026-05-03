package ru.practicum.ewm.stats.collector.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionProducer {

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @Value("${kafka.topics.user-actions:stats.user-actions.v1}")
    private String topic;

    public void send(UserActionAvro action) {
        kafkaTemplate.send(topic, action.getUserId(), action);
        log.debug("Sent user action: userId={}, eventId={}, type={}",
            action.getUserId(), action.getEventId(), action.getActionType());
    }
}
