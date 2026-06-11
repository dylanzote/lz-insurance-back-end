package com.lz_Insurance.testcontainers.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TestcontainersConfig.class)
public @interface EnableTestcontainers {
}
