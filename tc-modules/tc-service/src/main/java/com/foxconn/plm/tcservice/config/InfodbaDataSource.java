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
@ConfigurationProperties(prefix = "spring.datasource.druid.infodba")
@MapperScan(basePackages = "com.foxconn.plm.tcservice.mapper.infodba", sqlSessionFactoryRef = "InfodbaDataSourceSqlSessionFactory")
public class InfodbaDataSource {
    private String url;
    private String username;
    private String password;

    @Bean(name = "InfodbaDataSource")
    public DataSource getDataSource() {
        DruidDataSource build = DruidDataSourceBuilder.create().build();
        build.setUrl(url);
        build.setUsername(username);
        build.setPassword(password);
        return build;
    }

    @Bean(name = "InfodbaDataSourceSqlSessionFactory")
    // @Qualifier表示查找Spring容器中名字为test1DataSource的对象
//    public SqlSessionFactory test1SqlSessionFactory(@Qualifier("test1DataSource") DataSource datasource, interceptor)
    public SqlSessionFactory sessionFactory(@Qualifier("InfodbaDataSource") DataSource datasource)
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(datasource);
//        bean.setPlugins(new Interceptor[]{interceptor});
        bean.setMapperLocations(
                // 设置mybatis的xml所在位置
                new PathMatchingResourcePatternResolver().getResources("classpath:mybatis/infodba/**/*.xml"));
        return bean.getObject();
    }

    @Bean("InfodbaSqlSessionTemplate")
    // 表示这个数据源是默认数据源
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(
            @Qualifier("InfodbaDataSourceSqlSessionFactory") SqlSessionFactory sessionFactory) {
        return new SqlSessionTemplate(sessionFactory);
    }


}
