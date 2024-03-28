package com.foxconn.plm.tcservice.tclicensereport.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName: HistoryRateRes
 * @Description:
 * @Author DY
 * @Create 2022/12/3
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class HistoryRateRes implements Serializable {
    private static final long serialVersionUID = 1L;
    @ApiModelProperty("歷史數據使用率統計結果")
    private List<LicenseRes> aurList;
    @ApiModelProperty("歷史數據稼動率統計結果")
    private List<LicenseRes> lurList;
}
