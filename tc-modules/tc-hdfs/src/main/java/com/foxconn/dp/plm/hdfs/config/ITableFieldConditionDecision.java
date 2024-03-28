package com.foxconn.dp.plm.hdfs.config;

/**
 * 表字段条件决策器
 * 用于决策某个表是否需要添加某个字段过滤条件
 **/
public interface ITableFieldConditionDecision {

    /**
     * 条件字段是否运行null值
     *
     * @return
     */
    boolean isAllowNullValue();

    /**
     * 忽悠假删除
     *
     * @return
     */
    boolean ignoreDelete(String tableName);

    /**
     * 部门权限
     *
     * @return
     */
    boolean authority(String tableName);

    /**
     * 判决某个表是否需要添加某个字段过滤
     *
     * @param tableName 表名称
     * @param fieldName 字段名称
     * @return
     */
    boolean adjudge(String tableName, String fieldName);
}
