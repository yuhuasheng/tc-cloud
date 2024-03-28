package com.foxconn.plm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.foxconn.plm.spas.mapper")
@SpringBootApplication(scanBasePackages = {"com.foxconn.plm.spas", "com.foxconn.plm.utils", "com.foxconn.plm.tcapi"})
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients(basePackages = {"com.foxconn.plm"})
public class TcIntegrateSpasApp {


    public static void main(String[] args) {
        SpringApplication.run(TcIntegrateSpasApp.class, args);
    }
}
