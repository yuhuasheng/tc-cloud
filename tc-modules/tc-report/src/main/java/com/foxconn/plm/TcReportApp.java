package com.foxconn.plm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author HuashengYu
 * @Date 2022/12/30 15:49
 * @Version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.foxconn.plm"})
public class TcReportApp {

    public static void main(String[] args) {
        SpringApplication.run(TcReportApp.class, args);
    }
}
