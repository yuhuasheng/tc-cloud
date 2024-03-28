package com.foxconn.plm.rabbitmq.dynamic;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.rabbitmq")
@Data
public class RabbitModuleProperties {

    private List<RabbitModuleInfo> modules;

}
