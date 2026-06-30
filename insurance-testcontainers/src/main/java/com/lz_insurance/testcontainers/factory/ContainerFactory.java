package com.lz_insurance.testcontainers.factory;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class ContainerFactory {

    private GenericContainer<?> postgres;
    private GenericContainer<?> kafka;
    private GenericContainer<?> minio;
    private GenericContainer<?> redis;

    public void startAll() {
        if (postgres == null) {
            postgres = new GenericContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withExposedPorts(5432)
                .withEnv("POSTGRES_DB", "insurance_test")
                .withEnv("POSTGRES_USER", "test")
                .withEnv("POSTGRES_PASSWORD", "test");
            postgres.start();
            log.info("Started PostgreSQL container: {}:{}", postgres.getHost(), postgres.getFirstMappedPort());
        }

        if (kafka == null) {
            kafka = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
                .withExposedPorts(9093)
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
                .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9093");
            kafka.start();
            log.info("Started Kafka container: {}:{}", kafka.getHost(), kafka.getFirstMappedPort());
        }

        if (minio == null) {
            minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                .withExposedPorts(9000, 9001)
                .withEnv("MINIO_ROOT_USER", "minioadmin")
                .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                .withCommand("server /data --console-address ':9001'");
            minio.start();
            log.info("Started MinIO container: {}:{}", minio.getHost(), minio.getFirstMappedPort());
        }

        if (redis == null) {
            redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
            redis.start();
            log.info("Started Redis container: {}:{}", redis.getHost(), redis.getFirstMappedPort());
        }
    }

    public void stopAll() {
        if (postgres != null) {
            postgres.stop();
            log.info("Stopped PostgreSQL container");
        }
        if (kafka != null) {
            kafka.stop();
            log.info("Stopped Kafka container");
        }
        if (minio != null) {
            minio.stop();
            log.info("Stopped MinIO container");
        }
        if (redis != null) {
            redis.stop();
            log.info("Stopped Redis container");
        }
    }

    public GenericContainer<?> getPostgres() {
        return postgres;
    }

    public GenericContainer<?> getKafka() {
        return kafka;
    }

    public GenericContainer<?> getMinio() {
        return minio;
    }

    public GenericContainer<?> getRedis() {
        return redis;
    }
}
