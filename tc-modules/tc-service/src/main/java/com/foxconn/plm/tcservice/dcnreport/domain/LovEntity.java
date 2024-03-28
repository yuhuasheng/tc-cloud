package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2022/10/22 9:29
 * @Version 1.0
 */
@Data
public class LovEntity {

    private String bu;
    private String customer;
    private String productLine;
    private String projectInfo;
    private List<LovEntity> childs;
    private LovEntity lovEntity;

    public LovEntity() {
        childs = Collections.synchronizedList(new ArrayList<>());
    }

    public void addChild(LovEntity child) {
        this.childs.add(child);
    }
}
