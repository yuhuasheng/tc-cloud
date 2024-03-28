package com.foxconn.plm.integrate.spas.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class Series extends Project implements Serializable {
    private String cid;
    private String productLineName;
}
