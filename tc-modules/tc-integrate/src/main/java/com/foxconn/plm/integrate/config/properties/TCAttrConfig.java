package com.foxconn.plm.integrate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "pnms")
public class TCAttrConfig {
    private Map<String, String> tcAttrMapping = new HashMap<>();
}
