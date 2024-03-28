package com.foxconn.plm.tcservice.connandcable.domain;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2023/01/31/ 16:48
 * @description
 */

@Data
public class CableInfo extends CoCaInfo{
    private String type = "Cable";
}
