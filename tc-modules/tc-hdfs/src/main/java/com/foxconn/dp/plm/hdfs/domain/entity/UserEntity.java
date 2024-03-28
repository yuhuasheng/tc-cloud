package com.foxconn.dp.plm.hdfs.domain.entity;

import lombok.Data;

@Data
public class UserEntity {
    String id;
    String name;
    String dept;
    String bu;
    // 3=Manager 5=PM Manager
    int role;

    public boolean isManager() {
        return role == 3 || role == 5;
    }
}
