package com.foxconn.plm.spas.bean;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ManpowerRawInfo {
       String snapId;
      String projectId;
      String deptName;
      String phase;
      Integer isActive;
      Float factor;
      Date createDate;
      Date updateDate;
}
