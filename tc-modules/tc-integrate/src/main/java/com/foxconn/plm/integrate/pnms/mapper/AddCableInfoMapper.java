package com.foxconn.plm.integrate.pnms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface AddCableInfoMapper {

    int getCableCountByHHPN(@Param("hhpn") String hhpn);

    String getHHPNEmptyByGroupId(@Param("groupId") String groupId);

    void addCableInfo(@Param("hhpn") String hhpn, @Param("designPN") String designPN,
                      @Param("desc") String desc, @Param("mfg") String mfg,
                      @Param("groupId") String groupId, @Param("creator") String creator,
                      @Param("creationTime") Date creationTime, @Param("modifyTime") Date modifyTime);

    void updateCableInfo(@Param("hhpn") String hhpn, @Param("designPN") String designPN,
                         @Param("desc") String desc, @Param("mfg") String mfg,
                         @Param("modifyTime") Date modifyTime, @Param("id") String id);
}
