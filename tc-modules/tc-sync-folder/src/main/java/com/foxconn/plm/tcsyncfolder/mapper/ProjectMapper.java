package com.foxconn.plm.tcsyncfolder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foxconn.plm.tcsyncfolder.entity.ProjectEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * @ClassName: ProjectMapper
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Mapper
public interface ProjectMapper extends BaseMapper<ProjectEntity> {
    Integer getId();
}
