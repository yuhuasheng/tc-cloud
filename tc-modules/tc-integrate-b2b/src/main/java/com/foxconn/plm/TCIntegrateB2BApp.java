package com.foxconn.plm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = {"com.foxconn"})
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients(basePackages = {"com.foxconn.plm"})
public class TCIntegrateB2BApp {
    public static void main(String[] args) {
        SpringApplication.run(TCIntegrateB2BApp.class, args);

    }
}
