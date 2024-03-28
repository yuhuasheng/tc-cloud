package com.foxconn.plm.tcservice.tclicensereport.service.impl;

import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.tcservice.mapper.master.UserMapper;
import com.foxconn.plm.tcservice.tclicensereport.domain.UserInfo;
import com.foxconn.plm.tcservice.tclicensereport.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public Date getMaxRecordDate() {
        return userMapper.getMaxRecordDate();
    }

    @Override
    public List<UserInfo> getTCUserInfo() {
        return userMapper.getTCUserInfo();
    }

    @Override
    public List<UserInfo> getYesterdayUserInfo(String yesterday) {
        return userMapper.getYesterdayUserInfo(Access.check(yesterday));
    }

    @Override
    public void setUserInfo(List<UserInfo> userInfoList) {
        userMapper.setUserInfo(Access.check(userInfoList));
    }

}
