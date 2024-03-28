package com.foxconn.plm.ops.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foxconn.plm.ops.entity.OperationEntity;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:16
 **/
public interface OperationService extends IService<OperationEntity> {

    Long getId();
}
