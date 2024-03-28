package com.foxconn.plm.tcservice.projectReport.dto.rv;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CumulativeOutput {

    private String name;
    public int shouldOutQty;
    public int outQty;
    private float rate;

    public CumulativeOutput(String name) {
        this.name = name;
    }
}
