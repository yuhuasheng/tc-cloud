package com.foxconn.plm.tcservice.projectReport.dto.rv;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FunctionOnlineDetail {

    private String name;
    public int online;
    public int notOnline;
    public int totalQty;
    private float rate;
    public int shouldOutQty;
    public int outQty;
    public int archivedQty;
    public int workflowDiagramDocumentQty;

    public FunctionOnlineDetail(String name) {
        this.name = name;
    }
}
