package com.foxconn.plm.ops.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foxconn.plm.ops.entity.OperationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:15
 **/
@Mapper
public interface OperationMapper extends BaseMapper<OperationEntity> {

    /**
     * 獲取下一條數據的id
     * @return
     */
    Long getId();
}
