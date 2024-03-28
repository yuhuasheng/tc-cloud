package com.foxconn.plm.integrate.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.foxconn.plm.integrate.config.properties.MinioPropertiesConfig;
import com.foxconn.plm.integrate.config.properties.TCFRMinioPropertiesConfig;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.Resource;


/**
 * @author: robert
 */
@Configuration
//@EnableConfigurationProperties({MinioPropertiesConfig.class, TCFRMinioPropertiesConfig.class})
public class MinioConfig {

    @Resource
    private MinioPropertiesConfig minioPropertiesConfig;

    @Resource
    private TCFRMinioPropertiesConfig tcfrMinioPropertiesConfig;
    /**
     * 初始化 MinIO 客户端
     */
    @Bean(value = "mdasMinioClient")
    @Primary
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioPropertiesConfig.getEndpoint())
                .credentials(minioPropertiesConfig.getAccessKey(), minioPropertiesConfig.getSecretKey())
                .build();
    }


    @Bean(value = "tcfrMinioClient")
    public MinioClient tcfrMinioClient() {
        return MinioClient.builder()
                .endpoint(tcfrMinioPropertiesConfig.getEndpoint())
                .credentials(tcfrMinioPropertiesConfig.getAccessKey(), tcfrMinioPropertiesConfig.getSecretKey())
                .build();
    }


    @Bean
    public Snowflake snowflake() {
        return IdUtil.getSnowflake(9, 9);
    }
}
