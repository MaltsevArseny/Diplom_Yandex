package ru.practicum.ewm.stats.collector.serializer;

import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.serializer.BaseAvroSerializer;

public class UserActionAvroSerializer extends BaseAvroSerializer<UserActionAvro> {

    public UserActionAvroSerializer() {
        super(UserActionAvro.class);
    }
}
