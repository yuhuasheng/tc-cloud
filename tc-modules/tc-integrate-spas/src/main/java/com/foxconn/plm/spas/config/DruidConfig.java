package com.foxconn.plm.spas.config;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import com.foxconn.plm.spas.config.dataSource.DataSourceType;
import com.foxconn.plm.spas.config.dataSource.DynamicDataSource;
import com.foxconn.plm.spas.config.properties.DruidProperties;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * druid 配置多数据源
 *
 * @author robert
 */
@Configuration
public class DruidConfig {

    private static ConfigurableListableBeanFactory beanFactory;

    @Bean(name = "masterDataSource")
    @ConfigurationProperties("spring.datasource.druid.master")
    public DataSource masterDataSource(DruidProperties druidProperties) {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }

    @Bean(name = "slaveDataSource")
    @ConfigurationProperties("spring.datasource.druid.slave")
    @ConditionalOnProperty(prefix = "spring.datasource.druid.slave", name = "enabled", havingValue = "true")
    public DataSource slaveDataSource(DruidProperties druidProperties) {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }


    @Bean(name = "cis")
    @ConfigurationProperties("spring.datasource.druid.cis")
    @ConditionalOnProperty(prefix = "spring.datasource.druid.cis", name = "enabled", havingValue = "true")
    public DataSource cisDataSource(DruidProperties druidProperties) {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }

    @Bean(name = "cisDell")
    @ConfigurationProperties("spring.datasource.druid.cisdell")
    @ConditionalOnProperty(prefix = "spring.datasource.druid.cisdell", name = "enabled", havingValue = "true")
    public DataSource cisDellDataSource(DruidProperties druidProperties) {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }

    @Bean(name = "dynamicDataSource")
    @Primary
    public DynamicDataSource dataSource(DataSource masterDataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER.name(), masterDataSource);
        setDataSource(targetDataSources, DataSourceType.SLAVE.name(), "slaveDataSource");
        setDataSource(targetDataSources, DataSourceType.CIS.name(), "cis");
        setDataSource(targetDataSources, DataSourceType.CISDELL.name(), "cisDell");
        return new DynamicDataSource(masterDataSource, targetDataSources);
    }

    /**
     * 设置数据源
     *
     * @param targetDataSources 备选数据源集合
     * @param sourceName        数据源名称
     * @param beanName          bean名称
     */
    public void setDataSource(Map<Object, Object> targetDataSources, String sourceName, String beanName) {
        try {
            DataSource dataSource = SpringUtil.getBean(beanName);
            targetDataSources.put(sourceName, dataSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
