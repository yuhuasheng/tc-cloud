package com.foxconn.plm.integrate.cis.mapper.cisdell;

import com.foxconn.plm.integrate.cis.domain.PartEntity;
import com.foxconn.plm.integrate.cis.domain.TCSyncBean;
import com.foxconn.plm.integrate.cis.domain.ThreeDDrawingBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CISDellMapper {
    public List<PartEntity> getNotSyncPart();

    public List<PartEntity> getPartById(int id);

    public void updateSync(int id);

    public List<ThreeDDrawingBean> getThreeDDrawingRecord();

}
