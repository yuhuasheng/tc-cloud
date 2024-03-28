package com.foxconn.plm.spas.service.impl;

import com.foxconn.plm.spas.bean.ProjectInfo;
import com.foxconn.plm.spas.mapper.SynSpasProjectStatusMapper;
import com.foxconn.plm.spas.service.SynSpasProjectStatusService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2023/02/14/ 11:11
 * @description
 */
@Service("synSpasProjectStatusServiceImpl")
public class SynSpasProjectStatusServiceImpl implements SynSpasProjectStatusService {

    @Resource
    private SynSpasProjectStatusMapper synSpasProjectStatusMapper;

    @Override
    public List<String> getProjectSPASIdAll() throws Exception {
        return synSpasProjectStatusMapper.getProjectSPASIdAll();
    }

    @Override
    public List<ProjectInfo> getProjectInfoById(List<String> projectIds) throws Exception {
        return synSpasProjectStatusMapper.getProjectInfoById(projectIds);
    }
}
