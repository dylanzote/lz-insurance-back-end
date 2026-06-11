package com.lz_Insurance.testcontainers.base;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class IntegrationTestBase {

    protected static GenericContainer<?> postgres;
    protected static GenericContainer<?> kafka;
    protected static GenericContainer<?> minio;
    protected static GenericContainer<?> redis;

    @BeforeAll
    static void startContainers() {
        postgres = new GenericContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_DB", "insurance_test")
            .withEnv("POSTGRES_USER", "test")
            .withEnv("POSTGRES_PASSWORD", "test");
        postgres.start();

        kafka = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withExposedPorts(9093)
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9093");
        kafka.start();

        minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
            .withExposedPorts(9000, 9001)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data --console-address ':9001'");
        minio.start();

        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
        redis.start();
    }

    @AfterAll
    static void stopContainers() {
        if (postgres != null) postgres.stop();
        if (kafka != null) kafka.stop();
        if (minio != null) minio.stop();
        if (redis != null) redis.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/insurance_test");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:" + kafka.getFirstMappedPort());
        registry.add("minio.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("minio.accessKey", () -> "minioadmin");
        registry.add("minio.secretKey", () -> "minioadmin");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getFirstMappedPort().toString());
    }
}
