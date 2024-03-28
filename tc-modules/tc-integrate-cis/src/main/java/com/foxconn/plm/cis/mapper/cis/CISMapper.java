package com.foxconn.plm.cis.mapper.cis;

import cn.hutool.core.date.DateTime;
import com.foxconn.plm.cis.domain.EE3DCISModelInfo;
import com.foxconn.plm.cis.domain.PartEntity;
import com.foxconn.plm.cis.domain.TCSyncBean;
import com.foxconn.plm.cis.domain.ThreeDDrawingBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface CISMapper {
    public List<PartEntity> getNotSyncPart(@Param("startTime") DateTime startTime, @Param("endTime") DateTime endTime);

    public List<PartEntity> getPartById(int id);

    public void updateSync(int id);

    public List<ThreeDDrawingBean> getThreeDDrawingRecord();

    List<TCSyncBean> getTCSyncRecord();

    Integer getSCHEMATICPARTRecord(@Param("productLineId") Integer productLineId, @Param("schematicPart") String schematicPart);

    Integer getPCBFOOTPRINTRecord(@Param("productLineId") Integer productLineId, @Param("pcbFootprint") String pcbFootprint);

    @Select("select count(*) from ThreeDDrawingRecord where ecad_alt_name like #{pn}")
    Integer getThreeDDrawingRecordCount(@Param("pn") String pn);

    @Select("select PART_TYPE from Material where STANDARD_PN like #{pn}")
    String getCISPartType(@Param("pn") String pn);

    List<EE3DCISModelInfo> getNoCISModelInfo(@Param("list") Set<String> list);
}
