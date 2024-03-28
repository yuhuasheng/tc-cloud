package com.foxconn.plm.integrate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "mdas")
public class MDASTypeConfig {
    private Map<String, String> typeConfig = new HashMap<>();
}
