package com.foxconn.plm.tcservice.projectReport.dto.rv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BUProjectOnlineDetail {
    String name;
    public int online;
    public int notOnline;
    public int total;

    public BUProjectOnlineDetail(String name) {
        this.name = name;
    }
}
