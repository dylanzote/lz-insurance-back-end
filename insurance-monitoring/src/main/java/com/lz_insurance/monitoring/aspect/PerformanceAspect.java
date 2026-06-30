package com.lz_insurance.monitoring.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PerformanceAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(timed)")
    public Object aroundTimed(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        String metricName = timed.value().isEmpty() 
            ? joinPoint.getSignature().toShortString() 
            : timed.value();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Object result = joinPoint.proceed();
            sample.stop(Timer.builder(metricName)
                .tag("status", "success")
                .tag("method", joinPoint.getSignature().getName())
                .register(meterRegistry));
            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder(metricName)
                .tag("status", "error")
                .tag("method", joinPoint.getSignature().getName())
                .tag("exception", e.getClass().getSimpleName())
                .register(meterRegistry));
            throw e;
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Timed {
        String value() default "";
    }
}
