package com.foxconn.plm.spas.config.dataSource;

/**
 * 数据源
 *
 * @author robert
 */
public enum DataSourceType {
    /**
     * 主库
     */
    MASTER,

    /**
     * 从库
     */
    SLAVE,

    CIS,

    CISDELL
}
