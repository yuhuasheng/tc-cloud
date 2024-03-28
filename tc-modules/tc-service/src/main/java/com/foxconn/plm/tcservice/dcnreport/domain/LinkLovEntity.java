package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2022/10/28 14:22
 * @Version 1.0
 */
@Data
public class LinkLovEntity {

    private String value;
    private List<LinkLovEntity> childs;

    public LinkLovEntity() {
        childs = Collections.synchronizedList(new ArrayList<>());
    }

    public void addChild(LinkLovEntity child) {
        this.childs.add(child);
    }
}
