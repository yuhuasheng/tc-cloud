package com.foxconn.plm.integrate.sap.customPN.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


@Mapper
public interface CustomPNMapper {

    public List<Map<String, Object>> getSqlByPartSource(@Param("partSource") String partSource);

    public List<Map<String, Object>> getSqlByMtlType(@Param("mtlType") String mtlType);

    public List<Map<String, Object>> getSqlByPlant(@Param("plantCode") String plantCode);

    public List<Map<String, Object>> getSqlByMtlGroup(@Param("mtlGrp") String mtlGrp);

    public List<Map<String, Object>> getSqlByPlantMtlType(@Param("mtlType") String mtlType, @Param("plantCode") String plantCode);

    public List<String> selectNeedPost(@Param("mtlType") String mtlType, @Param("vCode") String vCode, @Param("plant") String plant);

    public List<String> selectMakerCode(@Param("makerName") String makerName);


    public abstract String getCustomSEQ(@Param("seqId") String seqId);

    public abstract void addSeqCounter(@Param("seqId") String seqId, @Param("seqCounter") String seqCounter);

    public abstract void updateSeqCounter(@Param("seqId") String seqId, @Param("seqCounter") String seqCounter);


}
