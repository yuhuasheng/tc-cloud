package com.foxconn.plm.cis.domain;

import lombok.Data;

import java.util.List;

@Data
public class EE3DProjectBean {
    private String value;

    private String bu;

    private String customer;

    private String projectSeries;

    private String projectName;

    private String id;

    private List<EE3DProjectBean> childs;
}
