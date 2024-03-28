package com.foxconn.plm.tcsyncfolder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foxconn.plm.tcsyncfolder.entity.FolderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @ClassName: FolderMapper
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Mapper
public interface FolderMapper extends BaseMapper<FolderEntity> {

    Integer getId();

    List<FolderEntity> getChildFolder(@Param("parentId") Integer parentId);
}
