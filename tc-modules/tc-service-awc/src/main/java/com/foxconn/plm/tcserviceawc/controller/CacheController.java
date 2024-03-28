package com.foxconn.plm.tcserviceawc.controller;

import cn.hutool.core.util.ObjectUtil;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.tcserviceawc.exception.CommonException;
import com.foxconn.plm.tcserviceawc.param.AddCacheParam;
import com.foxconn.plm.tcserviceawc.param.GetCacheParam;
import com.foxconn.plm.tcserviceawc.response.CacheRes;
import com.foxconn.plm.tcserviceawc.service.BoCacheService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 緩存前端控制器
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/2 9:25
 **/
@Validated
@CrossOrigin
@RestController
@RequestMapping("cache")
public class CacheController {
    @Resource
    private BoCacheService service;

    @GetMapping
    public R<CacheRes> getCache(@Validated GetCacheParam param){
        CacheRes res = service.getCache(param);
        return ObjectUtil.isNotNull(res) ? R.success(res) : R.error(HttpResultEnum.NO_RESULT.getCode(),HttpResultEnum.NO_RESULT.getMsg());
    }

    @PostMapping("save")
    public R<Boolean> saveCache(@Validated @RequestBody AddCacheParam param) throws CommonException {
        return service.saveCache(param) ? R.success(true) : R.success(false);
    }

    @DeleteMapping("del")
    public R<Boolean> deleteCache(@Validated @RequestBody GetCacheParam param) throws CommonException {
        return service.deleteCache(param) ? R.success(true) : R.success(false);
    }
}
