package com.foxconn.plm.tcsyncfolder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foxconn.plm.tcsyncfolder.entity.ProjectEntity;
import com.foxconn.plm.tcsyncfolder.mapper.ProjectMapper;
import com.foxconn.plm.tcsyncfolder.service.ProjectService;
import com.foxconn.plm.tcsyncfolder.vo.ProjectVo;
import org.springframework.stereotype.Service;

/**
 * @ClassName: ProjectServiceImpl
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, ProjectEntity> implements ProjectService {

    @Override
    public Integer getId() {
        return baseMapper.getId();
    }
}
