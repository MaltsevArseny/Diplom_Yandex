package ru.practicum.ewm.stats.aggregator.serializer;

import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.serializer.BaseAvroDeserializer;

public class UserActionAvroDeserializer extends BaseAvroDeserializer<UserActionAvro> {

    public UserActionAvroDeserializer() {
        super(UserActionAvro.class);
    }
}
