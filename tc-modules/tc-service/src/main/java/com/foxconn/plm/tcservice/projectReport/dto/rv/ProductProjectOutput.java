package com.foxconn.plm.tcservice.projectReport.dto.rv;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductProjectOutput {

    private String name;
    public int archivedQty;
    public int workflowDiagramDocumentQty;

    public ProductProjectOutput(String name) {
        this.name = name;
    }
}
