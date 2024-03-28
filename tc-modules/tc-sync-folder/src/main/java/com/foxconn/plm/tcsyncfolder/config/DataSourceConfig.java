package com.foxconn.plm.tcsyncfolder.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import lombok.Data;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

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
