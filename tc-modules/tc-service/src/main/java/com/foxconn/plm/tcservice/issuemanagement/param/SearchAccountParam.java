package com.foxconn.plm.tcservice.issuemanagement.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 搜索賬號參數類
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/21 11:07
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchAccountParam implements Serializable {
    @ApiParam(value = "工號")
    private String no;
    @ApiParam(value = "名稱")
    private String name;
    @ApiParam(value = "bu")
    private String bu;
    @ApiParam(value = "platform")
    private String platform;
    @ApiParam(value = "部門")
    private String dept;
    @ApiParam(value = "一級賬號uid")
    private String tcUid;
    @ApiParam(value = "分页参数，当前页码，默认为1")
    private Integer pageNum = 1;
    @ApiParam("分页参数，当前页数据条数，默认为10条")
    private Integer pageSize = 10;
}
