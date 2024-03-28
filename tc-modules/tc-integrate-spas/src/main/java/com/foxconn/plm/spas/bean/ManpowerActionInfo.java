package com.foxconn.plm.spas.bean;

import lombok.Data;

import java.util.List;

@Data
public class ManpowerActionInfo {
    String projectId;
    String deptName;
     List<String> addPhaseNames;
     List<String> deletePhaseNames;
}
