package com.lz_insurance.storage.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region = "us-east-1";
    private boolean secure = false;
    private int connectTimeout = 10000;
    private int writeTimeout = 60000;
    private int readTimeout = 10000;
}
