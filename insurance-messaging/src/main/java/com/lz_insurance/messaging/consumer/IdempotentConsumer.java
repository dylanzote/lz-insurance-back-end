package com.lz_insurance.messaging.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IdempotentConsumer<T> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IdempotentConsumer.class);
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();
    private static final int MAX_PROCESSED_IDS = 10000;

    @KafkaListener(topics = "${kafka.topic}")
    public void consume(@Payload T message,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.RECEIVED_KEY) String key,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                       @Header(KafkaHeaders.OFFSET) long offset,
                       Acknowledgment acknowledgment) {
        
        String messageId = generateMessageId(topic, partition, offset);
        
        if (processedMessageIds.contains(messageId)) {
            log.warn("Duplicate message detected: {}", messageId);
            acknowledgment.acknowledge();
            return;
        }
        
        try {
            processMessage(message);
            processedMessageIds.add(messageId);
            
            if (processedMessageIds.size() > MAX_PROCESSED_IDS) {
                processedMessageIds.clear();
            }
            
            acknowledgment.acknowledge();
            log.debug("Successfully processed message: {}", messageId);
        } catch (Exception e) {
            log.error("Error processing message: {}", messageId, e);
            throw e;
        }
    }

    protected abstract void processMessage(T message);

    private String generateMessageId(String topic, int partition, long offset) {
        return String.format("%s-%d-%d", topic, partition, offset);
    }
}
