package com.foxconn.plm.entity.response;

import lombok.Data;

import java.util.Date;

/**
 * @Author HuashengYu
 * @Date 2022/8/3 10:04
 * @Version 1.0
 */
@Data
public class SPASUser {

    private String id;
    private String workId;//工号
    private String name;//姓名
    private String notes;//邮箱
    private String deptName;//部门名称
    private String isActive;//是否可用
    private Date createdTime;//创建时间
    private Date lastUpdateTime;//最后修改时间

}
