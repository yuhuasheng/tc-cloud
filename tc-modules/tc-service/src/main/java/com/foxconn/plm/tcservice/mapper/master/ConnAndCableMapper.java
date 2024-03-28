package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.connandcable.domain.CableBean;
import com.foxconn.plm.tcservice.connandcable.domain.CableInfo;
import com.foxconn.plm.tcservice.connandcable.domain.ConnectorInfo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2022/10/6 16:38
 * @Version 1.0
 */
public interface ConnAndCableMapper {

    void insertConnRecord(@Param("list") List<CableBean> connBeanList);

    void insertCableRecord(@Param("list") List<CableBean> cableBeanList);

    Integer checkConnRepeat(@Param("groupId") Integer groupId, @Param("HHPN") String HHPN);

    Integer checkCableRepeat(@Param("groupId") Integer groupId, @Param("HHPN") String HHPN);

    Integer getMAXGroupId();

    List<ConnectorInfo> getConnData(@Param("groupIds") List<String> groupIds);

    List<CableInfo> getCableData(@Param("groupIds") List<String> groupIds);

    List<String> queryConnGroupIdByPN(@Param("hhpns")List<String> hhpns);

    List<String> queryCableGroupIdByPN(@Param("hhpns")List<String> hhpns);

    Integer queryConnInfoCount(@Param("hhpn") String hhpn);

    Integer queryCableInfoCount(@Param("hhpn") String hhpn);

    Integer addConn(@Param("hhpn")String hhpn, @Param("desc")String desc, @Param("mfg")String mfg,
            @Param("groupId")String groupId, @Param("creator")String creator,
            @Param("creationTime") String creationTime, @Param("modifyTime")String modifyTime);

    Integer queryHHPNEmptyByGroupId(@Param("groupId") String groupId);

    Integer updateCable1(@Param("id")Integer id, @Param("hhpn")String hhpn, @Param("designPN")String designPN,
                         @Param("desc")String desc, @Param("mfg")String mfg);

    Integer addCable(@Param("hhpn")String hhpn, @Param("designPN")String designPN, @Param("desc")String desc,
                     @Param("mfg")String mfg, @Param("groupId")String groupId, @Param("creator")String creator,
                     @Param("creationTime") String creationTime, @Param("modifyTime")String modifyTime);

    Integer queryMaxGroupId(String forPassCheck);

    void addConnInfo(@Param("hhpn")String hhpn, @Param("desc")String desc,
                        @Param("mfg")String mfg, @Param("groupId")String groupId,
                        @Param("creator")String creator, @Param("creationTime") String creationTime, @Param("modifyTime")String modifyTime);

    void addCableInfo(@Param("groupId")String groupId, @Param("creator")String creator,
                         @Param("creationTime")String creationTime, @Param("modifyTime")String modifyTime);

    void updateConn(@Param("hhpn")String hhpn, @Param("desc")String desc, @Param("mfg")String mfg);

    void updateCable2(@Param("hhpn")String hhpn, @Param("desc")String desc, @Param("mfg")String mfg);

    void delConnInfo(@Param("connIds")List<String> connIds);

    void delCableInfo(@Param("cableIds")List<String> cableIds);
}
