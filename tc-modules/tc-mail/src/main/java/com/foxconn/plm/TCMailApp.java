package com.foxconn.plm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @Classname TCMailApp
 * @Description
 * @Date 2022/1/4 19:21
 * @Created by HuashengYu
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TCMailApp {

    public static void main(String[] args) {
        SpringApplication.run(TCMailApp.class, args);
    }
}
