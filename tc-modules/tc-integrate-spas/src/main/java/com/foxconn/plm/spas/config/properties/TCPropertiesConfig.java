package com.foxconn.plm.spas.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author robert
 */
@Data
@Component
@ConfigurationProperties(prefix = "tc")
public class TCPropertiesConfig {
    private String connectUrl;
    private String userName;
    private String password;
    private String fmsUrl;
}