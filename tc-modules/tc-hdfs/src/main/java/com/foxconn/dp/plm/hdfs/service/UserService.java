package com.foxconn.dp.plm.hdfs.service;


import com.foxconn.dp.plm.hdfs.domain.entity.FolderEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.ItemRevEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.UserEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.*;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface UserService {


    List<UserEntity> getUserInfoInSpas(List<String> empIds);


}
