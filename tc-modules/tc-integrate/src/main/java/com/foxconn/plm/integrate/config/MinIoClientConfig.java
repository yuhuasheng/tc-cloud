package com.foxconn.plm.integrate.config;

import io.minio.MinioClient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName: MinIoClientConfig
 * @Description:
 * @Author DY
 * @Create 2022/12/15
 */
@Data
@EqualsAndHashCode
@Configuration
public class MinIoClientConfig {
    @Value("${lbs.minio.endpoint}")
    private String endpoint;
    @Value("${lbs.minio.accessKey}")
    private String accessKey;
    @Value("${lbs.minio.secretKey}")
    private String secretKey;
    @Value("${lbs.minio.bucketName}")
    private String bucketName;

    @Bean("LbsMinioClient")
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
