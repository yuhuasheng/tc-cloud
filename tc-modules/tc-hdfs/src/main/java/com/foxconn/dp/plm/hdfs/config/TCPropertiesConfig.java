package com.foxconn.dp.plm.hdfs.config;

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
    private String userNameWh;
    private String passwordWh;
    private String userNameCq;
    private String passwordCq;
    private String userNameHsinchu;
    private String passwordHsinchu;
    private String userNameTpe;
    private String passwordTpe;
    private String userNameLh;
    private String passwordLh;
    private String fmsUrl;

}
