package com.foxconn.dp.plm.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.sql.SQLException;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "com.foxconn.plm.feign")
public class TcHealthApplication {
    public static void main(String[] args) {
        SpringApplication.run(TcHealthApplication.class, args);
    }

}
