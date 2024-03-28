package com.foxconn.dp.plm.fileservice.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.foxconn.dp.plm.privately.FileServerPropertitesUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * druid 配置据源
 */
@Configuration
public class DruidConfig {

    @Bean(name = "dataSource")
    public DataSource druidDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        //设置连接参数
        dataSource.setUrl(FileServerPropertitesUtils.getProperty("jdbc.url"));
        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        dataSource.setUsername(FileServerPropertitesUtils.getProperty("jdbc.username"));
        dataSource.setPassword(FileServerPropertitesUtils.getProperty("jdbc.pwd"));
        //配置初始化大小、最小、最大
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        //连接泄漏监测
        dataSource.setRemoveAbandoned(true);
        dataSource.setRemoveAbandonedTimeout(30);
        //配置获取连接等待超时的时间
        dataSource.setMaxWait(20000);
        //配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        dataSource.setTimeBetweenEvictionRunsMillis(20000);
        //防止过期
        dataSource.setValidationQuery("SELECT 1 from dual");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(true);

        return dataSource;

    }


}
