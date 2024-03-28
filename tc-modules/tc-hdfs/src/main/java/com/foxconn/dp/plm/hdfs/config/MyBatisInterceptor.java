package com.foxconn.dp.plm.hdfs.config;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class
        }),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class MyBatisInterceptor implements Interceptor {

    private SqlConditionHelper conditionHelper = new SqlConditionHelper(new ITableFieldConditionDecision() {
        @Override
        public boolean isAllowNullValue() {
            return true;
        }

        @Override
        public boolean ignoreDelete(String tableName) {
            if (tableName.toUpperCase(Locale.ENGLISH).startsWith("js_".toUpperCase(Locale.ENGLISH))) {
                return true;
            }
            return false;
        }

        @Override
        public boolean authority(String tableName) {
            return true;
        }

        @Override
        public boolean adjudge(String tableName, String fieldName) {
            return true;
        }
    });
    ;

    /**
     * 仅用于打印完整sql
     *
     * @param invocation
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        System.out.println("+++++++++++++++++++++sql:+++++++++++++++++++++");
        System.out.println(boundSql.getSql());
        return invocation.proceed();
    }

    /**
     * 未使用，仅作参考
     *
     * @param invocation
     * @return
     * @throws Throwable
     */
    public Object interceptBak(Invocation invocation) throws Throwable {
//        System.out.println("+++++++++++++++++++++进入拦截器+++++++++++++++++++++");
//        BaseVm baseVm = BaseVmUtil.getBaseVm();
//        String tenantId="";
//        if (baseVm != null){
//            tenantId = baseVm.getCorpCode();
//        }
//        //租户id为空时不做处理
//        if (StringUtils.isBlank(tenantId)) {
//            System.out.println(baseVm);
//            System.out.println("+++++++++++++++ baseVm == null++++++++++++++++++");
//            return invocation.proceed();
//        }
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();

        MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = null;
        if (statementHandler instanceof RoutingStatementHandler) {
            StatementHandler delegate = (StatementHandler) ReflectionUtils
                    .getFieldValue(statementHandler, "delegate");
            mappedStatement = (MappedStatement) ReflectionUtils.getFieldValue(
                    delegate, "mappedStatement");
        } else {
            mappedStatement = (MappedStatement) ReflectionUtils.getFieldValue(
                    statementHandler, "mappedStatement");
        }
        String namespace = mappedStatement.getId();
        String className = namespace.substring(0, namespace.lastIndexOf("."));
        String methodName = namespace.substring(namespace.lastIndexOf(".") + 1);
        methodName = methodName.replace("_COUNT", "");
        Method[] ms = Class.forName(className).getMethods();
//        System.out.println("类名：" + namespace);
//        System.out.println("方法名：" + methodName);
//        for(Method m : ms){
//            if((m.isAnnotationPresent(IgnoreAuthority.class) && m.getName().equals(methodName))){
//                System.out.println("跳过方法名："+ m.getName());
//                return invocation.proceed();
//            }
//        }
        BoundSql boundSql = statementHandler.getBoundSql();
//        System.out.println("+++++++++++++++++++++old sql:+++++++++++++++++++++");
        System.out.println(boundSql.getSql());
        String newSql = addCondition(boundSql.getSql());
//        System.out.println("++++++++++++++++++++++new sql:++++++++++++++++++++");
        System.out.println(newSql);
//        System.out.println("+++++++++++++++++++++离开拦截器+++++++++++++++++++++");
        metaStatementHandler.setValue("delegate.boundSql.sql", newSql);

        return invocation.proceed();
    }

    private String addCondition(String sql) {
        List<SQLStatement> statementList = SQLUtils.parseStatements(sql, "mysql");
        if (statementList == null || statementList.size() == 0) {
            return sql;
        }
        for (SQLStatement statement : statementList) {
            conditionHelper.addStatementCondition(statement, "IS_DELETED", "0");
        }
        return SQLUtils.toSQLString(statementList, "mysql");
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

}

