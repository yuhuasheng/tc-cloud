package com.foxconn.plm.tcsyncfolder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foxconn.plm.tcsyncfolder.entity.ProjectEntity;
import com.foxconn.plm.tcsyncfolder.vo.ProjectVo;

/**
 * @ClassName: ProjectService
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
public interface ProjectService extends IService<ProjectEntity> {
    Integer getId();
}
