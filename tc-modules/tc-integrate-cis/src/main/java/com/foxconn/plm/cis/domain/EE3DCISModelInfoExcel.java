package com.foxconn.plm.cis.domain;

import com.foxconn.plm.entity.constants.TCPropName;
import lombok.Data;

@Data
public class EE3DCISModelInfoExcel {

    @TCPropName(cell = 0)
    private String item;
    @TCPropName(cell = 1, isMerge = true)
    private String bu;
    @TCPropName(cell = 2, isMerge = true)
    private String customer;
    @TCPropName(cell = 3, isMerge = true)
    private String projectSeries;
    @TCPropName(cell = 4, isMerge = true)
    private String projectName;
    @TCPropName(cell = 5, isMerge = true)
    private String phase;
    @TCPropName(cell = 6, isMerge = true)
    private String version;
    @TCPropName(cell = 7, isMerge = true)
    private String cisLibrary;
    @TCPropName(cell = 8, isMerge = true)
    private String partType;
    @TCPropName(cell = 9)
    private String hhPn;
    @TCPropName(cell = 10)
    private String StandardPn;
    @TCPropName(cell = 11)
    private String mfg;
    @TCPropName(cell = 12)
    private String mfgPn;
    @TCPropName(cell = 13)
    private String processName;
    @TCPropName(cell = 14)
    private String processStatus;
    @TCPropName(cell = 15)
    private String cisCustomer;
    @TCPropName(cell = 16)
    private String department;
    @TCPropName(cell = 17)
    private String wfNode;
    @TCPropName(cell = 18)
    private String reMark;
}

