package com.foxconn.plm.spas.mapper;

import com.foxconn.plm.spas.bean.ProjectPersonl;
import com.foxconn.plm.spas.bean.SynSpasChangeData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/08/ 16:15
 * @description
 */
@Mapper
public interface SynSpasChangeDataMapper {

    Integer querySynSpasChangeDataRecord(Integer id);

    void addSynSpasChangeData(List<SynSpasChangeData> synSpasChangeData);

    List<ProjectPersonl> querySpasTML(@Param("bu") String bu, @Param("customerName") String customerName);
}
