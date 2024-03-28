package com.foxconn.plm.integrate.log.domain;

import lombok.Data;

import java.util.List;

@Data
public class ManpowerInfo {
    String deptName;
    List<String> phases;
}
