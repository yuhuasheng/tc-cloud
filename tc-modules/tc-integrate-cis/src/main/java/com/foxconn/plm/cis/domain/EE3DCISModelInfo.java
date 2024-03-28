package com.foxconn.plm.cis.domain;

import com.foxconn.plm.entity.constants.TCPropName;
import lombok.Data;

@Data
public class EE3DCISModelInfo {
    @TCPropName(cell = 0)
    private String hhPn;
    @TCPropName(cell = 1)
    private String StandardPn;
    @TCPropName(cell = 2)
    private String mfg;
    @TCPropName(cell = 3)
    private String mfgPn;
    @TCPropName(cell = 4)
    private String partType;
    @TCPropName(cell = 5)
    private String processName;
    @TCPropName(cell = 6)
    private String processStatus;
    @TCPropName(cell = 7)
    private String cisCustomer;
    @TCPropName(cell = 8)
    private String department;
    @TCPropName(cell = 9)
    private String reMark;

    private String productLine;

    private String wfNode;

    private String cisLibrary;

    private String url;

    private String tcCustomer;

    private String tcProjectSeries;


}

