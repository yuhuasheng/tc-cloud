package com.foxconn.plm.tcservice.projectReport.dto.rv;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeptEmailBean {

    private String bu;
    private String customer;
    private String dept;
    private String username;
    private String email;
    private String filePath;

}
