package com.foxconn.plm.tcserviceawc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foxconn.plm.tcserviceawc.entity.BoCacheEntity;
import com.foxconn.plm.tcserviceawc.exception.CommonException;
import com.foxconn.plm.tcserviceawc.param.AddCacheParam;
import com.foxconn.plm.tcserviceawc.param.GetCacheParam;
import com.foxconn.plm.tcserviceawc.response.CacheRes;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/2 9:24
 **/
public interface BoCacheService extends IService<BoCacheEntity> {
    CacheRes getCache(GetCacheParam param);

    Boolean saveCache(AddCacheParam param) throws CommonException;

    boolean deleteCache(GetCacheParam param) throws CommonException;
}
