package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author MW00333
 * @Date 2023/5/6 15:28
 * @Version 1.0
 */
@Data
public class LinkFeeLovEntity {

    private String value;
    private List<LinkFeeLovEntity> childs;

    public LinkFeeLovEntity() {
        childs = Collections.synchronizedList(new ArrayList<>());
    }

    public void addChild(LinkFeeLovEntity child) {
        this.childs.add(child);
    }
}
