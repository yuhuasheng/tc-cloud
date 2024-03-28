package com.foxconn.plm.integrate.lbs.mapper;

import cn.hutool.core.date.DateTime;
import com.foxconn.plm.integrate.lbs.domain.SyncRes;
import com.foxconn.plm.integrate.lbs.entity.LbsSyncEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @ClassName: LbsSyncMapper
 * @Description:
 * @Author DY
 * @Create 2022/12/15
 */
@Mapper
public interface LbsSyncMapper {
    int saveEntity(LbsSyncEntity entity);

    List<LbsSyncEntity> getByTime(@Param("startTime") DateTime startTime, @Param("endTime") DateTime endTime);

    int updateByIds(@Param("ids") List<Long> ids);

    int updateById(@Param("id") Long id);

    List<SyncRes> getList();

    int batchDelete(@Param("ids") List<String> ids);
}
