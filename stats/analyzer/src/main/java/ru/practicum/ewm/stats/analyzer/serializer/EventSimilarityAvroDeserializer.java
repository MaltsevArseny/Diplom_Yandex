package ru.practicum.ewm.stats.analyzer.serializer;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.serializer.BaseAvroDeserializer;

public class EventSimilarityAvroDeserializer extends BaseAvroDeserializer<EventSimilarityAvro> {

    public EventSimilarityAvroDeserializer() {
        super(EventSimilarityAvro.class);
    }
}
