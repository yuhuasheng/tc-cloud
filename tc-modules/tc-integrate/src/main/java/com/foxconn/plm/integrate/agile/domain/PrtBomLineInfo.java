package com.foxconn.plm.integrate.agile.domain;

import lombok.Data;

@Data
public class PrtBomLineInfo {
    private String parentNum;
    private String childNum;
    private String findNum;
    private String qty;
    private String altCode;
    private String altGroup;


}
