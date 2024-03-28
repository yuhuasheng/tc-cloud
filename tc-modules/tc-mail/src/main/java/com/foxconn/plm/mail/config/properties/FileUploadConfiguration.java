package com.foxconn.plm.mail.config.properties;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;

/**
 * @Classname FileUploadConfiuration
 * @Description 设置文件上传的大小控制(配置类)
 * @Date 2022/3/2 17:17
 * @Created by HuashengYu
 */
@Configuration
public class FileUploadConfiguration {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //单个文件大小50mb
        factory.setMaxFileSize(DataSize.ofMegabytes(50L));
        //设置总上传数据大小10GB
        factory.setMaxRequestSize(DataSize.ofGigabytes(10L));
        return factory.createMultipartConfig();
    }
}
