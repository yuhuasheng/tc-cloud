package com.foxconn.plm.tcreport.reportsearchparams.domain;

import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Author HuashengYu
 * @Date 2023/1/3 17:07
 * @Version 1.0
 */
@Data
public class LinkLovBean {

    private String value;
    private List<LinkLovBean> childs;

    public LinkLovBean() {
        childs = new CopyOnWriteArrayList<>();
    }

    public void addChild(LinkLovBean child) {
        this.childs.add(child);
    }
}
