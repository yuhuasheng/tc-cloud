package com.foxconn.dp.plm.hdfs.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import lombok.Data;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.datasource.druid.master")
@MapperScan(basePackages = "com.foxconn.dp.plm.hdfs.dao.xplm", sqlSessionFactoryRef = "test2SqlSessionFactory")
public class DataSourceConfig2 {
    private String url;
    private String username;
    private String password;

    @Bean(name = "test2DataSource")
    public DataSource getDateSource2() {
        DruidDataSource build = DruidDataSourceBuilder.create().build();
        build.setUrl(url);
        build.setUsername(username);
        build.setPassword(password);
        return build;
    }

    @Bean(name = "test2SqlSessionFactory")
    public SqlSessionFactory test2SqlSessionFactory(@Qualifier("test2DataSource") DataSource datasource)
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(datasource);
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mybatis/xplm/*.xml"));
        return bean.getObject();
    }


    @Bean("test2SqlSessionTemplate")
    public SqlSessionTemplate test2SqlSessionTemplate(
            @Qualifier("test2SqlSessionFactory") SqlSessionFactory sessionFactory) {
        return new SqlSessionTemplate(sessionFactory);
    }


}
