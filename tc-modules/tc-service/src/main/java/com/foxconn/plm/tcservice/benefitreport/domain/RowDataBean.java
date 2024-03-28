package com.foxconn.plm.tcservice.benefitreport.domain;

import com.foxconn.plm.entity.constants.TCPropName;
import lombok.Data;

/**
 * @Author HuashengYu
 * @Date 2022/10/11 9:46
 * @Version 1.0
 */
@Data
public class RowDataBean {
    @TCPropName(cell = 5)
    private String functionName;
    @TCPropName(cell = 0)
    private String bu;
    private String dept;
    @TCPropName(cell = 6)
    private String creator;
    private String createName;
    @TCPropName(cell = 2)
    private String itemId;
    @TCPropName(cell = 3)
    private String ver;
    private String projectId;
    @TCPropName(cell = 1)
    private String projectName;
    @TCPropName(cell = 4)
    private String levels;
    private String phase;
    @TCPropName(cell = 7)
    private String startTime;
    @TCPropName(cell = 8)
    private String endTime;
    private String phaseEndDate;
}
