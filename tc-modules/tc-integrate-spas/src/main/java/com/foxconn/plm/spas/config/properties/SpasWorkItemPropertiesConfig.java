package com.foxconn.plm.spas.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/12/ 14:16
 * @description
 */
@Data
@Component
@ConfigurationProperties(prefix = "spas.workitem")
public class SpasWorkItemPropertiesConfig {
    private String host;
    private String username;
    private String password;
    private String apiKey;
    private String sysFlag;//1 同步中间表  0暂停同步中间表(SPAS中间表 => TC中间表)
}
