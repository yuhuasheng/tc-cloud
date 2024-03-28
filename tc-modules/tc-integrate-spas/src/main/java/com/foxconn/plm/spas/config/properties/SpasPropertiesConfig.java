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
@ConfigurationProperties(prefix = "spas")
public class SpasPropertiesConfig {
    private String userName;
    private String password;
    private String admin;
    private String nameSpace;
    private String isSync;//1 同步中间表  0暂停同步中间表(SPAS中间表 => TC中间表)
    private String syncExist;//1 同步TC已有的专案  0同步所有专案
    private String url;
    private String sysFlag;
    private String apiKey;
    private String userName1;
    private String password1;
}
