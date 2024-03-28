package com.foxconn.plm.tcservice.tclicensereport.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @Author HuashengYu
 * @Date 2022/9/20 11:24
 * @Version 1.0
 */
@Data
public class DateRecordInfo {

    @JsonFormat(pattern = "yyyy/MM/dd")
    private Date recordDate;

    private String workingDayMainland;

    private String workingDayTaiwan;
}
