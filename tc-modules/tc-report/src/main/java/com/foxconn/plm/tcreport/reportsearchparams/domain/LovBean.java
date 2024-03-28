package com.foxconn.plm.tcreport.reportsearchparams.domain;

import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Author HuashengYu
 * @Date 2023/1/3 16:38
 * @Version 1.0
 */
@Data
public class LovBean implements Comparable<LovBean> {

    private String bu;
    private String customer;
    private String productLine;
    private String projectSeries;
    private String projectInfo;
    private String phase;
    private String chassis;


    @Override
    public int compareTo(LovBean o) {
        int i = this.bu.compareTo(o.getBu());
        if (i == 0) {
            int j = this.customer.compareTo(o.getCustomer());
            if (j == 0) {
                int k = this.productLine.compareTo(o.getProductLine());
                if (k == 0) {
                    int m = this.projectSeries.compareTo(o.getProjectSeries());
                    if (m == 0) {
                        return this.projectInfo.compareTo(o.getProjectInfo());
                    } else {
                        return m;
                    }
                } else {
                    return k;
                }
            } else {
                return j;
            }
        }
        return i;
    }
}
