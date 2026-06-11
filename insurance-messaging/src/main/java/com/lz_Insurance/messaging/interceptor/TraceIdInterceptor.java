package com.lz_Insurance.messaging.interceptor;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TraceIdInterceptor implements ProducerListener<Object, Object> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TraceIdInterceptor.class);
    private static final String TRACE_ID_HEADER = "traceId";

    @Override
    public void onSuccess(ProducerRecord<Object, Object> producerRecord, RecordMetadata recordMetadata) {
        log.debug("Message sent successfully to topic: {}, partition: {}, offset: {}",
            recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
    }

    @Override
    public void onError(ProducerRecord<Object, Object> producerRecord, RecordMetadata recordMetadata, Exception exception) {
        log.error("Error sending message to topic: {}", producerRecord.topic(), exception);
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    public static ProducerRecord<Object, Object> addTraceId(ProducerRecord<Object, Object> record) {
        String traceId = generateTraceId();
        record.headers().add(TRACE_ID_HEADER, traceId.getBytes());
        return record;
    }
}
