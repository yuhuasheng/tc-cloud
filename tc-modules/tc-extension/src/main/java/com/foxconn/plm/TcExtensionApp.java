package com.foxconn.plm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@MapperScan(basePackages = {"com.foxconn.plm.extension.avl.mapper"})
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients
public class TcExtensionApp {
    public static void main(String[] args) {
        SpringApplication.run(TcExtensionApp.class, args);


    }
}
