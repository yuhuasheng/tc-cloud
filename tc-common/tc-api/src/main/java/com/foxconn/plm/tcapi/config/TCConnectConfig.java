package com.foxconn.plm.tcapi.config;


import com.foxconn.plm.entity.config.TCConnectUserConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Robert
 */
@Data
@Component
@ConfigurationProperties(prefix = "tc.connect")
public class TCConnectConfig {
    private String fmsUrl;
    private String connectUrl;
    private Map<String, TCConnectUserConfig> userConfig = new HashMap<>();
}
