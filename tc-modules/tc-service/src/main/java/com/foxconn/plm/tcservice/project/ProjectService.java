package com.foxconn.plm.tcservice.project;

import com.foxconn.plm.tcservice.mapper.infodba.ProjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {

    @Resource
    ProjectMapper mapper;

    public List<ProjectBean> queryProjectByPrivilegeUser(String userId) {
        return mapper.queryProjectByPrivilegeUser(userId);
    }

}
