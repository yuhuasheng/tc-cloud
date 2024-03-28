package com.foxconn.plm.spas.service;

import com.foxconn.plm.spas.bean.ProjectInfo;

import java.util.List;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2023/02/14/ 11:09
 * @description
 */
public interface SynSpasProjectStatusService {

    List<String> getProjectSPASIdAll() throws Exception;

    List<ProjectInfo> getProjectInfoById(List<String> projectIds) throws Exception;

}
