package com.foxconn.plm.ops.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foxconn.plm.ops.entity.OperationEntity;
import com.foxconn.plm.ops.mapper.OperationMapper;
import com.foxconn.plm.ops.service.OperationService;
import org.springframework.stereotype.Service;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:16
 **/
@Service
public class OperationServiceImpl extends ServiceImpl<OperationMapper, OperationEntity> implements OperationService {
    @Override
    public Long getId() {
        return baseMapper.getId();
    }
}
