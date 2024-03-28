package com.foxconn.plm.mail.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author HuashengYu
 * @Date 2022/3/28 10:08
 * @Version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "tc-mail")
public class TCMailPropertiesConfig {

    private String host;
    private String port;
    private String administratorEmail;
}
