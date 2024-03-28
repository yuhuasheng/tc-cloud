package com.foxconn.plm.tcserviceawc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foxconn.plm.tcserviceawc.entity.BoCacheEntity;
import com.foxconn.plm.tcserviceawc.entity.BoCachePropEntity;
import com.foxconn.plm.tcserviceawc.exception.CommonException;
import com.foxconn.plm.tcserviceawc.mapper.BoCacheMapper;
import com.foxconn.plm.tcserviceawc.param.AddCacheParam;
import com.foxconn.plm.tcserviceawc.param.GetCacheParam;
import com.foxconn.plm.tcserviceawc.response.CacheRes;
import com.foxconn.plm.tcserviceawc.service.BoCachePropService;
import com.foxconn.plm.tcserviceawc.service.BoCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/2 9:25
 **/
@Service
public class BoCacheServiceImpl extends ServiceImpl<BoCacheMapper, BoCacheEntity> implements BoCacheService {
    @Resource
    private Snowflake snowflake;
    @Resource
    private BoCachePropService propService;

    @Override
    public CacheRes getCache(GetCacheParam param) {
        BoCacheEntity one = this.getOne(new QueryWrapper<BoCacheEntity>().lambda()
                .eq(BoCacheEntity::getActualUser, param.getActualUser())
                .eq(BoCacheEntity::getHigherObject, param.getHigherObject())
                .eq(BoCacheEntity::getActive, "0")
        );
        if(ObjectUtil.isNull(one)){
            return null;
        }
        CacheRes res = new CacheRes();
        BeanUtil.copyProperties(one,res);
        res.setCacheTime(one.getCreateTime());
        Map<String,String> props = new HashMap<>();
        // 查詢屬性
        List<BoCachePropEntity> list = propService.list(new QueryWrapper<BoCachePropEntity>().lambda().eq(BoCachePropEntity::getCacheId, one.getId()));
        for (BoCachePropEntity entity : list) {
            props.put(entity.getPropKey(),entity.getPropValue());
        }
        res.setProps(props);
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveCache(AddCacheParam param) throws CommonException {
        if(CollUtil.isEmpty(param.getProps())){
            throw CommonException.exception_400("緩存的屬性不能為空");
        }
        // 講之前的所有緩存都設置成非激活狀態
        BoCacheEntity update = new BoCacheEntity();
        update.setActive("1");
        this.update(update, new QueryWrapper<BoCacheEntity>().lambda()
                .eq(BoCacheEntity::getActualUser, param.getActualUser())
                .eq(BoCacheEntity::getHigherObject, param.getHigherObject())
                .eq(BoCacheEntity::getActive, "0"));
        // 新增數據
        BoCacheEntity entity = new BoCacheEntity();
        BeanUtil.copyProperties(param,entity);
        entity.setId(snowflake.nextId());
        entity.setActive("0");
        entity.setDelFlag("0");
        // 設置屬性
        List<BoCachePropEntity> list = new ArrayList<>();
        for (String key : param.getProps().keySet()) {
            BoCachePropEntity propEntity = new BoCachePropEntity();
            propEntity.setId(snowflake.nextId());
            propEntity.setCacheId(entity.getId());
            propEntity.setPropKey(key);
            propEntity.setPropValue(param.getProps().get(key));
            list.add(propEntity);
        }
        return this.save(entity) && propService.saveBatch(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCache(GetCacheParam param) throws CommonException {
        List<BoCacheEntity> list = this.list(new QueryWrapper<BoCacheEntity>().lambda()
                .eq(BoCacheEntity::getActualUser, param.getActualUser())
                .eq(BoCacheEntity::getHigherObject, param.getHigherObject())
        );
        if(CollUtil.isEmpty(list)){
            throw CommonException.exception_201("未查詢到緩存的數據");
        }
        return this.remove(new QueryWrapper<BoCacheEntity>().lambda()
                .eq(BoCacheEntity::getActualUser, param.getActualUser())
                .eq(BoCacheEntity::getHigherObject, param.getHigherObject()));
    }
}
