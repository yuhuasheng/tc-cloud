package com.foxconn.plm.tcservice.ebom.domain;


import com.foxconn.plm.entity.constants.TCPropName;

public class MntChangeSheet
{
    public final static int    START_ROW = 6;
    public final static String TEMPLATE  = "templates/MNT_BOM_Change_List.xlsx";
    @TCPropName(row = 2, cell = 1)
    private String             model;

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }
}
