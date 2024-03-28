package com.foxconn.plm.integrate.spas.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class Project implements Serializable {
    private String id;
    private Integer dataSourcesId;
    private String folderId;
    private String name;
    private String operationType;
    private String handleResults;
    private String exceptionMessage;
    private Date creationTime;
    private Date lastModifyTime;
}