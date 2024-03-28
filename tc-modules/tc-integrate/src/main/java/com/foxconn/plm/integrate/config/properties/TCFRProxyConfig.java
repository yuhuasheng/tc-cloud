package com.foxconn.plm.integrate.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author MW00333
 * @Date 2023/4/12 17:24
 * @Version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "teams.proxy")
public class TCFRProxyConfig {

    private String serverProxyIp;
    private String clientProxtIp;
    private String port;
    private String taskStatusUrl;
    private String projectInfoParamsUrl;
    private String customerParamsUrl;
    private String meetingTypeParamsUrl;
    private String tcUserParamsUrl;
    private String meetingFileParamsUrl;
}
