package com.foxconn.dp.plm.hdfs.config;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.util.JdbcConstants;
import com.github.pagehelper.util.StringUtil;

import java.util.List;

/**
 * This is SqlConditionHelper file.
 *
 * @author fp
 * @date Created in 2020/2/23 14:00
 * @package com.example.demo.interceptor
 * @org www.maxnerva.com {云智汇(重庆)高新科技服务有限公司}
 * @copyright 2019 Maxnerva
 */
public class SqlConditionHelper {
    private final ITableFieldConditionDecision conditionDecision;

    public SqlConditionHelper(ITableFieldConditionDecision conditionDecision) {
        this.conditionDecision = conditionDecision;
    }

    /**
     * 为sql'语句添加指定where条件
     *
     * @param sqlStatement
     * @param fieldName
     * @param fieldValue
     */
    public void addStatementCondition(SQLStatement sqlStatement, String fieldName, String fieldValue) {
        if (sqlStatement instanceof SQLSelectStatement) {
            SQLSelect select = ((SQLSelectStatement) sqlStatement).getSelect();
            if (select.getQuery() instanceof SQLUnionQuery) {
                SQLUnionQuery query = (SQLUnionQuery) select.getQuery();
                for (SQLSelectQuery relation : query.getRelations()) {
                    addSelectStatementCondition((SQLSelectQueryBlock) relation, ((SQLSelectQueryBlock) relation).getFrom(), fieldName, fieldValue);
                }
                return;
            }

            SQLSelectQueryBlock queryObject = (SQLSelectQueryBlock) select.getQuery();

            // 获取字段列表
            List<SQLSelectItem> selectItems = queryObject.getSelectList();
            for (SQLSelectItem selectItem : selectItems) {
                if (selectItem.getExpr() instanceof SQLQueryExpr) {
                    SQLSelect subQuery = ((SQLQueryExpr) selectItem.getExpr()).getSubQuery();
                    SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) subQuery.getQuery();
                    addSelectStatementCondition(sqlSelectQueryBlock, sqlSelectQueryBlock.getFrom(), fieldName, fieldValue);
                }
            }
            addSelectStatementCondition(queryObject, queryObject.getFrom(), fieldName, fieldValue);
        } else if (sqlStatement instanceof SQLUpdateStatement) {
            SQLUpdateStatement updateStatement = (SQLUpdateStatement) sqlStatement;
            addUpdateStatementCondition(updateStatement, fieldName, fieldValue);
        } else if (sqlStatement instanceof SQLDeleteStatement) {
            SQLDeleteStatement deleteStatement = (SQLDeleteStatement) sqlStatement;
            addDeleteStatementCondition(deleteStatement, fieldName, fieldValue);
        } else if (sqlStatement instanceof SQLInsertStatement) {
            SQLInsertStatement insertStatement = (SQLInsertStatement) sqlStatement;
            if (conditionDecision.adjudge(insertStatement.getTableName().toString(), fieldName)) {
                addInsertStatementCondition(insertStatement, fieldName, fieldValue);
            }

        }
    }

    /**
     * 为insert语句添加where条件
     *
     * @param insertStatement
     * @param fieldName
     * @param fieldValue
     */
    private void addInsertStatementCondition(SQLInsertStatement insertStatement, String fieldName, String fieldValue) {
//        if (insertStatement != null) {
//            if (!insertStatement.getColumns().contains(new SQLIdentifierExpr(fieldName))) {
//                insertStatement.addColumn(new SQLIdentifierExpr(fieldName));
//                insertStatement.getValuesList().forEach(valuesClause -> {
//                    valuesClause.addValue(new SQLIdentifierExpr(fieldValue));
//                });
//                BaseVm baseVm = BaseVmUtil.getBaseVm();
//                // 如果前端传入参数officeCode
//                if (baseVm != null && !StringUtil.isEmpty(baseVm.getUserCode())) {
//                    if (!insertStatement.getColumns().contains(new SQLIdentifierExpr("CREATOR"))) {
//                        insertStatement.addColumn(new SQLIdentifierExpr("CREATOR"));
//                        insertStatement.getValuesList().forEach(valuesClause -> {
//                            valuesClause.addValue(new SQLCharExpr(baseVm.getUserCode()));
//                        });
//                    }
//                }
//
//                if (!insertStatement.getColumns().contains(new SQLIdentifierExpr("CREATED_DT"))) {
//                    insertStatement.addColumn(new SQLIdentifierExpr("CREATED_DT"));
//                    insertStatement.getValuesList().forEach(valuesClause -> {
//                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//                        valuesClause.addValue(new SQLCharExpr(LocalDateTime.now().format(formatter)));
//                    });
//                }
//                if (!insertStatement.getColumns().contains(new SQLIdentifierExpr("IS_DELETED"))) {
//                    insertStatement.addColumn(new SQLIdentifierExpr("IS_DELETED"));
//                    insertStatement.getValuesList().forEach(valuesClause -> {
//                        valuesClause.addValue(new SQLBooleanExpr(false));
//                    });
//                }
//            }
//
//            SQLSelect sqlSelect = insertStatement.getQuery();
//            if (sqlSelect != null) {
//                SQLSelectQueryBlock selectQueryBlock = (SQLSelectQueryBlock) sqlSelect.getQuery();
//                addSelectStatementCondition(selectQueryBlock, selectQueryBlock.getFrom(), fieldName, fieldValue);
//            }
//        }
    }


    /**
     * 为delete语句添加where条件
     *
     * @param deleteStatement
     * @param fieldName
     * @param fieldValue
     */
    private void addDeleteStatementCondition(SQLDeleteStatement deleteStatement, String fieldName, String fieldValue) {
        SQLExpr where = deleteStatement.getWhere();
        //添加子查询中的where条件
        addSQLExprCondition(where, fieldName, fieldValue);

        SQLExpr newCondition = newEqualityCondition(deleteStatement.getTableName().getSimpleName(),
                deleteStatement.getTableSource().getAlias(), fieldName, fieldValue, where);
        deleteStatement.setWhere(newCondition);

    }

    /**
     * where中添加指定筛选条件
     *
     * @param where      源where条件
     * @param fieldName
     * @param fieldValue
     */
    private void addSQLExprCondition(SQLExpr where, String fieldName, String fieldValue) {
        if (where instanceof SQLInSubQueryExpr) {
            SQLInSubQueryExpr inWhere = (SQLInSubQueryExpr) where;
            SQLSelect subSelectObject = inWhere.getSubQuery();
            SQLSelectQueryBlock subQueryObject = (SQLSelectQueryBlock) subSelectObject.getQuery();
            addSelectStatementCondition(subQueryObject, subQueryObject.getFrom(), fieldName, fieldValue);
        } else if (where instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr opExpr = (SQLBinaryOpExpr) where;
            SQLExpr left = opExpr.getLeft();
            SQLExpr right = opExpr.getRight();
            addSQLExprCondition(left, fieldName, fieldValue);
            addSQLExprCondition(right, fieldName, fieldValue);
        } else if (where instanceof SQLQueryExpr) {
            SQLSelectQueryBlock selectQueryBlock = (SQLSelectQueryBlock) (((SQLQueryExpr) where).getSubQuery()).getQuery();
            addSelectStatementCondition(selectQueryBlock, selectQueryBlock.getFrom(), fieldName, fieldValue);
        }
    }

    /**
     * 为update语句添加where条件
     *
     * @param updateStatement
     * @param fieldName
     * @param fieldValue
     */
    private void addUpdateStatementCondition(SQLUpdateStatement updateStatement, String fieldName, String fieldValue) {
        // 更新固定字段
//        if (updateStatement != null) {
//            List<SQLUpdateSetItem> items = updateStatement.getItems();
//            if (!CollectionUtils.isEmpty(items)) {
//                BaseVm baseVm = BaseVmUtil.getBaseVm();
//                // 最后编辑人
//                SQLUpdateSetItem lastEditorItem = new SQLUpdateSetItem();
//                lastEditorItem.setColumn(new SQLIdentifierExpr("last_editor"));
//                lastEditorItem.setValue(new SQLCharExpr(baseVm.getUserCode()));
//                items.add(lastEditorItem);
//
//                // 最后编辑时间
//                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//                SQLUpdateSetItem lastEditedDtItem = new SQLUpdateSetItem();
//                lastEditedDtItem.setColumn(new SQLIdentifierExpr("last_edited_dt"));
//                lastEditedDtItem.setValue(new SQLCharExpr(LocalDateTime.now().format(formatter)));
//                items.add(lastEditedDtItem);
//            }
//        }

        SQLExpr where = updateStatement.getWhere();
        //添加子查询中的where条件
        addSQLExprCondition(where, fieldName, fieldValue);
        SQLExpr newCondition = newEqualityCondition(updateStatement.getTableName().getSimpleName(),
                updateStatement.getTableSource().getAlias(), fieldName, fieldValue, where);
        updateStatement.setWhere(newCondition);
    }

    /**
     * 给一个查询对象添加一个where条件
     *
     * @param queryObject
     * @param fieldName
     * @param fieldValue
     */
    private void addSelectStatementCondition(SQLSelectQueryBlock queryObject, SQLTableSource from, String fieldName, String fieldValue) {
        if (StringUtil.isEmpty(fieldName) || from == null || queryObject == null) {
            return;
        }

        if (from instanceof SQLExprTableSource) {
            SQLExpr originCondition = queryObject.getWhere();
            String tableName = ((SQLIdentifierExpr) ((SQLExprTableSource) from).getExpr()).getName();
            String alias = from.getAlias();
            SQLExpr newCondition = null;
            if (!conditionDecision.ignoreDelete(tableName)) {
                newCondition = newEqualityCondition(tableName, alias, fieldName, fieldValue, originCondition);
            }
//            BaseVm baseVm = BaseVmUtil.getBaseVm();
            // 如果前端传入参数officeCode
//            if (baseVm != null && StringUtil.isEmpty(baseVm.getOfficeCode())
//                    && conditionDecision.authority(tableName)) {
//                newCondition = newLikeCondition(tableName, alias, "OFFICE_CODE", baseVm.getOfficeCode(), newCondition);
//            }

            queryObject.setWhere(newCondition);
        } else if (from instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinObject = (SQLJoinTableSource) from;
            SQLTableSource left = joinObject.getLeft();
            SQLTableSource right = joinObject.getRight();
            SQLExpr newCondition = null;
            if (!conditionDecision.ignoreDelete(right.toString())) {
                newCondition = newEqualityCondition(right.toString(), right.getAlias(), fieldName, fieldValue, null);
            }

            joinObject.addCondition(newCondition);
            addSelectStatementCondition(queryObject, left, fieldName, fieldValue);
            //addSelectStatementCondition(queryObject, right, fieldName, fieldValue);
        } else if (from instanceof SQLSubqueryTableSource) {
            SQLSelect subSelectObject = ((SQLSubqueryTableSource) from).getSelect();
            SQLSelectQueryBlock selectQueryBlock = (SQLSelectQueryBlock) subSelectObject.getQuery();

            // 获取字段列表
            List<SQLSelectItem> selectItems = selectQueryBlock.getSelectList();
            for (SQLSelectItem selectItem : selectItems) {
                if (selectItem.getExpr() instanceof SQLQueryExpr) {
                    SQLSelect subQuery = ((SQLQueryExpr) selectItem.getExpr()).getSubQuery();
                    SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) subQuery.getQuery();
                    try {
                        addSelectStatementCondition(sqlSelectQueryBlock, sqlSelectQueryBlock.getFrom(), fieldName, fieldValue);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            SQLSelectQueryBlock subQueryObject = (SQLSelectQueryBlock) subSelectObject.getQuery();
            addSelectStatementCondition(subQueryObject, subQueryObject.getFrom(), fieldName, fieldValue);
        } else {
            try {
                throw new Exception("未处理的异常");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据原来的condition创建一个新的condition
     *
     * @param tableName       表名称
     * @param tableAlias      表别名
     * @param fieldName
     * @param fieldValue
     * @param originCondition
     * @return
     */
    private SQLExpr newEqualityCondition(String tableName, String tableAlias, String fieldName, String fieldValue, SQLExpr originCondition) {
        //如果不需要设置条件
        if (!conditionDecision.adjudge(tableName, fieldName)) {
            return originCondition;
        }
        //如果条件字段不允许为空
        if (fieldValue == null && !conditionDecision.isAllowNullValue()) {
            return originCondition;
        }

        String filedName = StringUtil.isEmpty(tableAlias) ? tableName + "." + fieldName : tableAlias + "." + fieldName;
        SQLExpr condition = new SQLBinaryOpExpr(new SQLIdentifierExpr(filedName), new SQLCharExpr(fieldValue), SQLBinaryOperator.Equality);
        return SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, originCondition);
    }

    private SQLExpr newLikeCondition(String tableName, String tableAlias, String fieldName, String fieldValue, SQLExpr originCondition) {
        String filedName = StringUtil.isEmpty(tableAlias) ? tableName + "." + fieldName : tableAlias + "." + fieldName;
        SQLExpr condition = new SQLBinaryOpExpr(new SQLIdentifierExpr(filedName), new SQLCharExpr(fieldValue + "%"), SQLBinaryOperator.Like);
        return SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, originCondition);
    }


    public static void main(String[] args) {
//        String sql = "select * from user s;select * from user s where s.name='333'  ";
        //       String sql = "select *,(select m from b) as m from user s where s.name='333'";
        String sql = "SELECT count(0)\n" +
                "FROM (\n" +
                " SELECT DISTINCT a.GR_NO, a.INSPECTION_DATE, a.M_COM_MATERIAL_ID\n" +
                "  , (\n" +
                "   SELECT b.MATERIAL_NO\n" +
                "   FROM m_com_material b\n" +
                "   WHERE b.M_COM_MATERIAL_ID = a.M_COM_MATERIAL_ID\n" +
                "  ) AS mComMaterialNo, a.VENDOR_NAME, a.DOC_STATUS, a.LOT_NO, a.MATERIAL_CLASS\n" +
                "  , b.M_COM_MATERIALTYPE_CODE AS materialClassCode\n" +
                "  , (\n" +
                "   SELECT c.DICT_NAME\n" +
                "   FROM m_data_dict c\n" +
                "   WHERE c.DICT_CODE = 'DRAFT'\n" +
                "    AND c.DICT_TYPE = 'SUBMIT_STATUS'\n" +
                "  ) AS docStatusName\n" +
                " FROM t_qom_iqc_main a\n" +
                "  LEFT JOIN m_com_materialtype b\n" +
                "  ON a.MATERIAL_CLASS = b.M_COM_MATERIALTYPE_ID\n" +
                "   AND b.CORP_CODE = '0'\n" +
                "   AND b.IS_DELETED = '0'\n" +
                " WHERE a.DOC_STATUS = ?\n" +
                "  AND a.CORP_CODE = '0'\n" +
                "  AND a.IS_DELETED = '0'\n" +
                ") table_count";
//      String sql = "select * from (select * from tab t where id = 2 and name = 'wenshao') s where s.name='333'";
//        String sql="select u.*,g.name from user u join user_group g on u.groupId=g.groupId where u.name='123'";
//        String sql="SELECT office_code,office_name,parent_code,extend_s1,extend_s2\n" +
//                "\t\tFROM js_sys_office\n" +
//                "\t \n" +
//                "         WHERE office_code in ( '1'\n" +
//                "            \n" +
//                "            ) \n" +
//                "        union\n" +
//                "         \n" +
//                "\t\tSELECT office_code,office_name,parent_code,extend_s1,extend_s2\n" +
//                "\t\tFROM js_sys_office\n" +
//                "\t \n" +
//                "         WHERE parent_codes like concat('%',?,'%')";
        //             String sql = "update user set name=? where id =(select id from user s)";
//        String sql = "delete from user where id = ( select id from user s )";
        //       String sql = "insert into user (id,name) select g.id,g.name from user_group g where id=1";
        // String sql = "insert into user (id,name) values (1,2),(3,4)";
        //       String sql = "select u.*,g.name from user u join (select * from user_group g  join user_role r on g.role_code=r.code  ) g on u.groupId=g.groupId where u.name='123'";
        // String sql = "update user set name=Tom,age=16,address=你猜";
        List<SQLStatement> statementList = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
//        BaseVm baseVm = new BaseVm();
//        baseVm.setFactoryCode("SDJN_01");
//        baseVm.setFactoryName("山东济南一分部");
//        baseVm.setCorpCode("0");
//        baseVm.setCorpName("JeeSite");
//        baseVm.setUserCode("user1_fgaa");
//        baseVm.setUserName("用户1");
//        baseVm.setOfficeCode("SDJN_02");
//        BaseVmUtil.setBaseVm(baseVm);
        //决策器定义
        SqlConditionHelper helper = new SqlConditionHelper(new ITableFieldConditionDecision() {
            @Override
            public boolean adjudge(String tableName, String fieldName) {
                return true;
            }

            @Override
            public boolean isAllowNullValue() {
                return false;
            }

            @Override
            public boolean ignoreDelete(String tableName) {
                return false;
            }

            @Override
            public boolean authority(String tableName) {
                return true;
            }
        });
        for (SQLStatement statement : statementList) {
            helper.addStatementCondition(statement, "CORP_CODE", "000000000000000");
        }
        //添加多租户条件，domain是字段ignc，yay是筛选值

        System.out.println("源sql：" + sql);
        System.out.println("修改后sql:" + SQLUtils.toSQLString(statementList, JdbcConstants.MYSQL));


    }
}

