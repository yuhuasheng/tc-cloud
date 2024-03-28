package com.foxconn.plm.tcservice.tclicensereport.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName: LicenseRes
 * @Description:
 * @Author DY
 * @Create 2022/12/3
 */
@Data
@EqualsAndHashCode
public class LicenseRes implements Serializable {
    private static final long serialVersionUID = 1L;
    @ApiModelProperty("統計的屬性")
    private String name;
    @ApiModelProperty("統計的數據列表")
    private List<StatisticsRes> list;

}
