package com.foxconn.plm.integrate.spas.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProjectPersonl extends Project implements Serializable {

    private String userNumber;
    private String userName;
    private String deptName;
}
