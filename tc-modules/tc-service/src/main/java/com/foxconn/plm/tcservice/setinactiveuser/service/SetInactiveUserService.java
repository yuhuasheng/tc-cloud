package com.foxconn.plm.tcservice.setinactiveuser.service;

import com.foxconn.plm.tcservice.setinactiveuser.domain.UserBean;

import java.util.List;

public interface SetInactiveUserService {

    public List<UserBean> getUserInfo(int days, List<String> excludeUsers);

    public List<UserBean> getUserInfoByIds(List<String> userIds);

    public void updateUserState(List<UserBean> users);

    public void setUserState(List<UserBean> users);
}
