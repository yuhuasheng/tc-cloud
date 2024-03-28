package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.tclicensereport.domain.UserInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

@Mapper
public interface UserMapper {

    Date getMaxRecordDate();

    List<UserInfo> getTCUserInfo();

    List<UserInfo> getYesterdayUserInfo(String yesterday);

    void setUserInfo(List<UserInfo> userInfoList);

}
