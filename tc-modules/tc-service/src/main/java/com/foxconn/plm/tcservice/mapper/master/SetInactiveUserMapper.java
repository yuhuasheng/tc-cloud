package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.setinactiveuser.domain.UserBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SetInactiveUserMapper {

    public List<UserBean> getUserInfo(@Param("days") int days, @Param("excludeUsers") List<String> excludeUsers);

    public List<UserBean> getUserInfoByIds(@Param("userIds") List<String> userIds);

    public void updateUserState(@Param("users") List<UserBean> users);

}
