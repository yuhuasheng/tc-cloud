package com.foxconn.plm;

import java.security.AccessController;
import java.util.LinkedHashMap;

import cn.hutool.cron.CronUtil;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.foxconn.plm.integrate.sap.customPN.utils.SAPConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.yaml.snakeyaml.Yaml;
import sun.security.action.GetPropertyAction;

import java.util.Properties;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}, scanBasePackages = {"com.foxconn"})
@EnableDiscoveryClient
//@EnableScheduling
@EnableFeignClients(basePackages = {"com.foxconn.plm"})
public class TcIntegrateApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(TcIntegrateApp.class, args);
        System.out.println(run.getEnvironment().getProperty("spring.cloud.nacos.config.server-addr"));
        try {
            String serverAddr = run.getEnvironment().getProperty("spring.cloud.nacos.config.server-addr");
            String dataId = "tc-integrate.yaml";
            String group = "DEFAULT_GROUP";
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
            properties.put(PropertyKeyConst.NAMESPACE, run.getEnvironment().getProperty("spring.cloud.nacos.config.namespace"));

            ConfigService configService = NacosFactory.createConfigService(properties);

            String content = configService.getConfig(dataId, group, 5000);
            LinkedHashMap<String, Object> sourceMap = null;
            String lineSeparator = AccessController.doPrivileged(new GetPropertyAction("line.separator"));
            content = content.replaceAll("\\r", "");
            content = content.replaceAll("\\n", lineSeparator);
            sourceMap = new Yaml().loadAs(content, LinkedHashMap.class);

            SAPConstants.SAP_IP = (String) ((LinkedHashMap) sourceMap.get("sap")).get("ip");
            SAPConstants.SAP_USERID = (String) ((LinkedHashMap) sourceMap.get("sap")).get("userid");
            SAPConstants.setSapSD((String) ((LinkedHashMap) sourceMap.get("sap")).get("password"));
            SAPConstants.SAP_SYSTEMNUMBER = (String) ((LinkedHashMap) sourceMap.get("sap")).get("system-number");

            SAPConstants.SAP_IP_888 = (String) ((LinkedHashMap) sourceMap.get("sap")).get("ip-888");
            SAPConstants.SAP_USERID_888 = (String) ((LinkedHashMap) sourceMap.get("sap")).get("userid-888");
            SAPConstants.setSapSD888((String) ((LinkedHashMap) sourceMap.get("sap")).get("password-888"));
            SAPConstants.SAP_SYSTEMNUMBER_888 = (String) ((LinkedHashMap) sourceMap.get("sap")).get("system-number-888");

            SAPConstants.SAP_IP_868 = (String) ((LinkedHashMap) sourceMap.get("sap")).get("ip-868");
            SAPConstants.SAP_USERID_868 = (String) ((LinkedHashMap) sourceMap.get("sap")).get("userid-868");
            SAPConstants.setSapSD868((String) ((LinkedHashMap) sourceMap.get("sap")).get("password-868"));
            SAPConstants.SAP_SYSTEMNUMBER_868 = (String) ((LinkedHashMap) sourceMap.get("sap")).get("system-number-868");

            System.out.println("------");
        } catch (Exception e) {
            System.out.print(e);
        }


    }


}
