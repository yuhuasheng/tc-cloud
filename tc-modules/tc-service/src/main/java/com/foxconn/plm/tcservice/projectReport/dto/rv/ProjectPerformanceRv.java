package com.foxconn.plm.tcservice.projectReport.dto.rv;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class ProjectPerformanceRv {

    @ApiModelProperty("各BU专案上线情况")
    private List<BUProjectOnlineDetail> buList;

    @ApiModelProperty("各客户专案上线情况")
    private List<CustomerProjectOnlineDetail> customerList;

    @ApiModelProperty("各產品專案階段累計產出協作情況")
    private List<CumulativeOutput> cumulativeOutputList;

    @ApiModelProperty("各產品專案產出物情況")
    private List<ProductProjectOutput> productProjectOutputList;

    @ApiModelProperty("DT Function 專案上線情況")
    private List<FunctionOnlineDetail> functionOnlineDetailList;

}
