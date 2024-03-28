package com.foxconn.plm.spas.mapper;

import com.foxconn.plm.spas.bean.ProjectInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2023/02/14/ 11:14
 * @description
 */
@Mapper
public interface SynSpasProjectStatusMapper {

    List<String> getProjectSPASIdAll() throws Exception;

    List<ProjectInfo> getProjectInfoById(List<String> projectIds) throws Exception;
}
