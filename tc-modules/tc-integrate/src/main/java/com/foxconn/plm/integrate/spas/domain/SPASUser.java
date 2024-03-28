package com.foxconn.plm.integrate.spas.domain;

import lombok.Data;

import java.util.Date;

@Data
public class SPASUser implements Comparable<SPASUser> {
    private String id;
    private String workId;//工号
    private String name;//姓名
    private String notes;//邮箱
    private String deptName;//部门名称
    private String isActive;//是否可用
    private String sectionName;
    private Date createdTime;//创建时间
    private Date lastUpdateTime;//最后修改时间

    @Override
    public int compareTo(SPASUser o) {
        int i = this.workId.compareTo(o.getWorkId());
        if (i == 0) {
            return this.name.compareTo(o.getName());
        }
        return i;
    }
}
