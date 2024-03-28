package com.foxconn.plm.sso.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @ClassName: DataSourceConfig
 * @Description:
 * @Author DY
 * @Create 2023/4/14
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.datasource.druid.master")
public class DataSourceConfig {
    private String url;
    private String username;
    private String password;

    @Bean("druidDataSource")
    public DataSource getDateSource() {
        DruidDataSource build = DruidDataSourceBuilder.create().build();
        build.setUrl(url);
        build.setUsername(username);
        build.setPassword(password);
        build.setDriverClassName("oracle.jdbc.OracleDriver");
        return build;
    }

}
