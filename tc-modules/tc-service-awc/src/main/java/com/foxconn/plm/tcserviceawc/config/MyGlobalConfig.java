package com.foxconn.plm.tcserviceawc.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局配置bean類
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/2 10:45
 **/
@Configuration
public class MyGlobalConfig {

    @Bean
    public Snowflake snowflake(){
        return IdUtil.getSnowflake(0,8);
    }


}
