package com.foxconn.plm.tcservice.tclicensereport.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName: HistoryRadarRes
 * @Description:
 * @Author DY
 * @Create 2022/12/3
 */
@Data
@EqualsAndHashCode
public class HistoryRadarRes implements Serializable {
    private static final long serialVersionUID = 1L;
    @ApiModelProperty("統計的部門")
    private String bu;
    @ApiModelProperty("統計的總計使用率")
    private String totalUtilizationRate;
    @ApiModelProperty("統計的總計稼動率")
    private String totalCropRate;
    @ApiModelProperty("每個子部門的使用率和稼動率")
    private List<FunctionRes> itemList;

}
