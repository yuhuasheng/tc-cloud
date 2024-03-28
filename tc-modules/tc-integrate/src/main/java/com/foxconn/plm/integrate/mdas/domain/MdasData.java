package com.foxconn.plm.integrate.mdas.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class MdasData {
    private String bu;
    private String category;
    private String type;
    private String subType;
    private int ecFlag;
    private String threadSize;
    private String threadLen;
    private String part;
    private String pic;
    private String doc;
    private String file;
    private String itemId;
    private String bucketName;
    private String vendor;
}
