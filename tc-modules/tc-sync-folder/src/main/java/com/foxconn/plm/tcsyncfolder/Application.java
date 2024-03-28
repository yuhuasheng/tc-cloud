package com.foxconn.plm.tcsyncfolder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @ClassName: Application
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@EnableFeignClients(basePackages = "com.foxconn.plm.feign.service")
@ComponentScan("com.foxconn.plm")
@SpringBootApplication
@EnableDiscoveryClient
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
