package com.foxconn.plm.extension.config.dataSource;


import java.lang.annotation.*;

/**
 * 自定义多数据源切换注解
 *
 * @author robert
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataSource {
    /**
     * 切换数据源名称
     */
    public DataSourceType value() default DataSourceType.MASTER;
}
