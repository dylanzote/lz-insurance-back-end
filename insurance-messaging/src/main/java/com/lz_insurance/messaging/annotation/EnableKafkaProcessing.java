package com.lz_insurance.messaging.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(KafkaProcessingConfig.class)
public @interface EnableKafkaProcessing {
}
