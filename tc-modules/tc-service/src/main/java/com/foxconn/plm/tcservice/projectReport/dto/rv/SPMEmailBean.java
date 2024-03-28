package com.foxconn.plm.tcservice.projectReport.dto.rv;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SPMEmailBean {

    private String bu;
    private String customer;
    private String spm;
    private String email;
    private String filePath;

}
