package com.foxconn.plm.tcservice.tcfr;

import lombok.Data;
import org.mapstruct.Mapper;

@Data
public class TFCRReportRp {
    private String bu;
    private String customer;
    private String productLine;
    private String project;
    private String date;
    private String weeks;
}
