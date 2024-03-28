package com.foxconn.plm.spas.bean;

import lombok.Data;

@Data
public class SpasActionHis {
    private String snapId;
    private String projectId;
    private String dept;
    private String phase;
    private String archive;
    private String resource;
    private String action;

}
