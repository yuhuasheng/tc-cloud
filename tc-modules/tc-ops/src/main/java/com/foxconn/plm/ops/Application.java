package com.foxconn.plm.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 啟動類
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/27 9:33
 **/
@EnableFeignClients(basePackages = "com.foxconn.plm.feign.service")
@ComponentScan("com.foxconn.plm")
@SpringBootApplication
@EnableDiscoveryClient
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
