package com.foxconn.dp.plm.hdfs.service.impl;

import com.foxconn.dp.plm.hdfs.dao.xplm.UserMapper;
import com.foxconn.dp.plm.hdfs.domain.entity.UserEntity;
import com.foxconn.dp.plm.hdfs.service.UserService;
import com.foxconn.dp.plm.privately.Access;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {


    @Resource
    UserMapper userMapper;

    @Override
    public List<UserEntity> getUserInfoInSpas(List<String> empIds) {
        return userMapper.getUserInfoInSpas(Access.check(empIds));
    }
}
