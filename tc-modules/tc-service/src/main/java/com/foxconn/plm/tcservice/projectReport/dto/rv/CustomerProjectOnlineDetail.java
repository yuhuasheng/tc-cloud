package com.foxconn.plm.tcservice.projectReport.dto.rv;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerProjectOnlineDetail {
    String name;
    public int online;
    public int notOnline;
    public int total;

    public CustomerProjectOnlineDetail(String name) {
        this.name = name;
    }
}
