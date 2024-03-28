package com.foxconn.plm.tcservice.tclicensereport.service;

import com.foxconn.plm.tcservice.tclicensereport.domain.UserInfo;

import java.util.Date;
import java.util.List;

public interface UserService {

    Date getMaxRecordDate();

    List<UserInfo> getTCUserInfo();

    List<UserInfo> getYesterdayUserInfo(String yesterday);

    void setUserInfo(List<UserInfo> userInfoList);
}
