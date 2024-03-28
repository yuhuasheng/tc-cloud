package com.foxconn.plm.entity.response;


import lombok.Data;

@Data
public class BURv   {

    private long id;
    private String customer;
    private long customerId;
    private String productLine;
    private long productLineId;
    private String bu;
    private long buId;

}
