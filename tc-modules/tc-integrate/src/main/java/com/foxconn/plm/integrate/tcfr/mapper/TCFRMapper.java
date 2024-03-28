package com.foxconn.plm.integrate.tcfr.mapper;

import cn.hutool.core.date.DateTime;
import com.foxconn.plm.integrate.lbs.domain.SyncRes;
import com.foxconn.plm.integrate.lbs.entity.LbsSyncEntity;
import com.foxconn.plm.integrate.tcfr.domain.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: LbsSyncMapper
 * @Description:
 * @Author DY
 * @Create 2022/12/15
 */
@Mapper
public interface TCFRMapper {


    List<TCFRProjectInfoPojo> getTCFRProjectInfoPojos() throws Exception;

    List<String> getTCProjectFolder(@Param("projectId") String projectId);

    TCUserBean getTCUserInfo(@Param("email") String email);

    MeetBean getTCFRDataByScheduleUid(String scheduleId);

    MeetBean getTCFRDataByDocumentUid(String documentUid);

    void insertTCFRData(MeetBean bean);

    List<MeetBean> getTCFRData(@Param("userName") String userName);

    Integer getTCFRDataCount(MeetBean bean);

    void updateTCFRFilePath(MeetBean bean);

    void updateScheduleUid(MeetBean bean);

    MeetBean getTCFRDataByTCUid(String scheduleUid);

    String getUserMail(String userName);

    String getWrokDayTpe(Date recordDate);

    String getWrokDayMainland(Date recordDate);

    void updateFlag(MeetBean bean);

    void updateDocumentInfo(MeetBean bean);

    void updateStatus(MeetBean bean);

    List<CustomerPojo> getCustomerLov();

    List<MeetingTypeBean> getMeetingTypeLov();

    void insertOrUpdateSpasToTC(@Param("list") List<SpasToTCBean> list);

    void deleteBySpasUserId(@Param("list") List<SpasToTCBean> list);

    List<TCUserBean> getTotalTCUserInfo(@Param("userName") String userName);
}
