package com.foxconn.plm.spas.bean;

import lombok.Data;

import java.util.Date;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/12/ 11:14
 * @description
 */
@Data
public class SynSpasHandleResults {
    private Integer id;
    private Integer state;
    private String exceptionMessage;
    private Date completeTime;
}
