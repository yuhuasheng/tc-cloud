package com.foxconn.plm.cis.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import com.foxconn.plm.cis.config.dataSource.DataSourceType;
import com.foxconn.plm.cis.config.dataSource.DynamicDataSource;
import com.foxconn.plm.cis.config.properties.DruidProperties;
import com.foxconn.plm.utils.spring.SpringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Bean(name = "xplm")
    @ConfigurationProperties("spring.datasource.druid.xplm")
    @ConditionalOnProperty(prefix = "spring.datasource.druid.xplm", name = "enabled", havingValue = "true")
    public DataSource xplmDataSource(DruidProperties druidProperties) {
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
    public DynamicDataSource dataSource(@Qualifier("cis") DataSource masterDataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.CIS.name(), masterDataSource);
        setDataSource(targetDataSources, DataSourceType.CISDELL.name(), "cisDell");
        setDataSource(targetDataSources, DataSourceType.XPLM.name(), "xplm");
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
            DataSource dataSource = SpringUtils.getBean(beanName);
            targetDataSources.put(sourceName, dataSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
