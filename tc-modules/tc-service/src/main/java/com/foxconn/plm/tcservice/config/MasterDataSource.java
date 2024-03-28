package com.foxconn.plm.tcservice.config;

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
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.datasource.druid.master")
@MapperScan(basePackages = "com.foxconn.plm.tcservice.mapper.master", sqlSessionFactoryRef = "MasterDataSourceSqlSessionFactory",sqlSessionTemplateRef = "MasterSqlSessionTemplate")
public class MasterDataSource {
    private String url;
    private String username;
    private String password;

    @Bean(name = "MasterDataSource")
    @Primary
    public DataSource getDateSource1() {
        DruidDataSource build = DruidDataSourceBuilder.create().build();
        build.setUrl(url);
        build.setUsername(username);
        build.setPassword(password);
        return build;
    }

    @Bean(name = "MasterDataSourceSqlSessionFactory")
    @Primary
    public SqlSessionFactory test1SqlSessionFactory(@Qualifier("MasterDataSource") DataSource datasource)
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(datasource);
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:/mybatis/master/**/*.xml"));
        return bean.getObject();
    }

    @Bean("MasterSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate test1SqlSessionTemplate(
            @Qualifier("MasterDataSourceSqlSessionFactory") SqlSessionFactory sessionFactory) {
        return new SqlSessionTemplate(sessionFactory);
    }


}
