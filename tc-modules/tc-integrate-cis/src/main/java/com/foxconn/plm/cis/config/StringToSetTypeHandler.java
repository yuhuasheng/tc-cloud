package com.foxconn.plm.cis.config;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StringToSetTypeHandler extends BaseTypeHandler<Set<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, Set<String> o, JdbcType jdbcType) throws SQLException {
        if (ArrayUtil.isNotEmpty(o)) {
            String jsonStr = JSONUtil.toJsonStr(o);
            preparedStatement.setObject(i, jsonStr.getBytes());
        }
    }

    @Override
    public Set<String> getNullableResult(ResultSet resultSet, String s) throws SQLException {
        Set<String> set = new HashSet<>();
        Blob blob = resultSet.getBlob(s);
        if (blob != null && blob.length() > 0) {
            String str = new String(blob.getBytes(1, (int) blob.length()));
            List<String> list = JSONUtil.toList(str, String.class);
            set.addAll(list);
        }
        return set;
    }

    @Override
    public Set<String> getNullableResult(ResultSet resultSet, int i) throws SQLException {
        Set<String> set = new HashSet<>();
        Blob blob = resultSet.getBlob(i);
        if (blob != null && blob.length() > 0) {
            String str = new String(blob.getBytes(1, (int) blob.length()));
            List<String> list = JSONUtil.toList(str, String.class);
            set.addAll(list);
        }
        return set;
    }

    @Override
    public Set<String> getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        Blob blob = callableStatement.getBlob(i);
        Set<String> set = new HashSet<>();
        if (blob != null && blob.length() > 0) {
            String str = new String(blob.getBytes(1, (int) blob.length()));
            List<String> list = JSONUtil.toList(str, String.class);
            set.addAll(list);
        }
        return set;
    }

}
