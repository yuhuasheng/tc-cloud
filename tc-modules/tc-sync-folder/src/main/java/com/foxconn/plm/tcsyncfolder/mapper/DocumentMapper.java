package com.foxconn.plm.tcsyncfolder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foxconn.plm.tcsyncfolder.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * @ClassName: DocumentMapper
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {
    Integer getId();
}
