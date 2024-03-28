package com.foxconn.dp.plm.fileservice;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}, scanBasePackages = {"com.foxconn"})
@EnableDiscoveryClient
@EnableScheduling
public class TcFileServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(TcFileServiceApp.class, args);


    }


}
