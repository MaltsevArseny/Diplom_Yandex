package ru.practicum.ewm.stats.aggregator.serializer;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.serializer.BaseAvroSerializer;

public class EventSimilarityAvroSerializer extends BaseAvroSerializer<EventSimilarityAvro> {

    public EventSimilarityAvroSerializer() {
        super(EventSimilarityAvro.class);
    }
}
