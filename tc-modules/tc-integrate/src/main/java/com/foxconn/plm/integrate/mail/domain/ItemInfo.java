package com.foxconn.plm.integrate.mail.domain;

import lombok.Data;

@Data
public class ItemInfo {

    private String uuid;
    private String itemName;
    private String itemId;
    private String dataSet;
    private String objType;
    private String itemCategory;

}
