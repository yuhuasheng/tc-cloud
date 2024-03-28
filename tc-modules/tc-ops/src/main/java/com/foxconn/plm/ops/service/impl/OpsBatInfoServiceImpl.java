package com.foxconn.plm.ops.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foxconn.plm.ops.entity.OpsBatInfoEntity;
import com.foxconn.plm.ops.mapper.OpsBatInfoMapper;
import com.foxconn.plm.ops.service.OpsBatInfoService;
import org.springframework.stereotype.Service;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:17
 **/
@Service
public class OpsBatInfoServiceImpl extends ServiceImpl<OpsBatInfoMapper, OpsBatInfoEntity> implements OpsBatInfoService {
    @Override
    public Long getId() {
        return baseMapper.getId();
    }
}
