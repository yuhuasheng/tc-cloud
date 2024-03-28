package com.foxconn.plm.tcsyncfolder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foxconn.plm.tcsyncfolder.entity.DocumentRevEntity;
import com.foxconn.plm.tcsyncfolder.vo.FileVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @ClassName: DocumentRevMapper
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Mapper
public interface DocumentRevMapper extends BaseMapper<DocumentRevEntity> {
    Integer getId();

    FileVo getFileInfo(@Param("docRevId") Integer docRevId);
}
