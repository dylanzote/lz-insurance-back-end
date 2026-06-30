package com.lz_insurance.messaging.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

public class JsonDeserializer<T> implements Deserializer<T> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonDeserializer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Class<T> targetType;

    public JsonDeserializer(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, targetType);
        } catch (Exception e) {
            log.error("Error deserializing message from topic: {}", topic, e);
            throw new RuntimeException("Failed to deserialize message", e);
        }
    }
}
