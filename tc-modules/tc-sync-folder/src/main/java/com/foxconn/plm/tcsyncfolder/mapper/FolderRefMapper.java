package com.foxconn.plm.tcsyncfolder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foxconn.plm.tcsyncfolder.entity.FolderRefEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * @ClassName: FolderRefMapper
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Mapper
public interface FolderRefMapper extends BaseMapper<FolderRefEntity> {
    Integer getId();
}
