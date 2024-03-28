package com.foxconn.plm.spas.bean;

import lombok.Data;

import java.util.List;

@Data
public class ManpowerInfo {
    String deptName;
    List<String> phases;
}
