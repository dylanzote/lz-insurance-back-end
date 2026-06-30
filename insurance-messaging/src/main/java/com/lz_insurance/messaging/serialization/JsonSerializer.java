package com.lz_insurance.messaging.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerializer<T> implements Serializer<T> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonSerializer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, T data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            log.error("Error serializing message for topic: {}", topic, e);
            throw new RuntimeException("Failed to serialize message", e);
        }
    }
}
