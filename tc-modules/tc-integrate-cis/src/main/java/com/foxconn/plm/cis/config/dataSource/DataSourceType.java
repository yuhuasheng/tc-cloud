package com.foxconn.plm.cis.config.dataSource;

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
     * <p>
     * SLAVE,
     */
    CIS,

    CISDELL,

    XPLM
}
