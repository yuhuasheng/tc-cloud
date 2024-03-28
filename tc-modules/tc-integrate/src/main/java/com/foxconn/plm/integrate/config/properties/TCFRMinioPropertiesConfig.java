package com.foxconn.plm.integrate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author MW00333
 * @Date 2023/3/17 10:08
 * @Version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "tcfr.minio")
public class TCFRMinioPropertiesConfig {

    /**
     * 端点
     */
    private String endpoint;
    /**
     * 用户名
     */
    private String accessKey;
    /**
     * 密码
     */
    private String secretKey;

    /**
     * 桶名称
     */
    private String bucketName;
}
