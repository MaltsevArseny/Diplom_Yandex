package ru.practicum.ewm.stats.serializer;

import org.apache.avro.data.TimeConversions;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class BaseAvroSerializer<T extends SpecificRecord> implements Serializer<T> {

    private final SpecificDatumWriter<T> writer;

    protected BaseAvroSerializer(Class<T> clazz) {
        SpecificData model = new SpecificData();
        model.addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());
        this.writer = new SpecificDatumWriter<>(SpecificData.get().getSchema(clazz), model);
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) return null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Avro serialization error for " + data.getClass().getSimpleName(), e);
        }
    }
}
