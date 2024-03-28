package com.foxconn.plm.cis.domain;

import com.foxconn.plm.entity.constants.TCPropName;
import lombok.Data;

@Data
public class EE3DUserPojo {

    @TCPropName(cell = 0)
    private String bu;

    @TCPropName(cell = 1)
    private String customer;

    @TCPropName(cell = 2)
    private String func;

    @TCPropName(cell = 3)
    private String name;

    @TCPropName(cell = 4)
    private String employee;

    @TCPropName(cell = 5)
    private String mailAddr;


}
