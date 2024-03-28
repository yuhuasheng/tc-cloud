package com.foxconn.plm.tcservice.benefitreport.domain;

import com.foxconn.plm.entity.response.SPASProject;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2022/7/13 11:29
 * @Version 1.0
 */
@Data
public class SPASProjectBean {
    private String customer;
    private String levels;
    private String phase;
    private String bu;
    private List<SPASProjectBean> childs;
    private List<SPASProject> list;
    private SPASProject spasProject;

    public SPASProjectBean() {
        childs = Collections.synchronizedList(new ArrayList<SPASProjectBean>());
        list = Collections.synchronizedList(new ArrayList<SPASProject>());
    }

    public void addChild(SPASProjectBean child) {
        this.childs.add(child);
    }

}
