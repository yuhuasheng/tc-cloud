package com.foxconn.plm.cis.mapper.cisdell;

import cn.hutool.core.date.DateTime;
import com.foxconn.plm.cis.domain.EE3DCISModelInfo;
import com.foxconn.plm.cis.domain.PartEntity;
import com.foxconn.plm.cis.domain.ThreeDDrawingBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface CISDellMapper {
    public List<PartEntity> getNotSyncPart(@Param("startTime") DateTime startTime, @Param("endTime") DateTime endTime);

    public List<PartEntity> getPartById(int id);

    public void updateSync(int id);

    public List<ThreeDDrawingBean> getThreeDDrawingRecord();

    List<EE3DCISModelInfo> getNoCISModelInfo(@Param("list") Set<String> list);

}
