package com.foxconn.plm.tcservice.connandcable.constant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/10/7 8:55
 * @Version 1.0
 */
public class ConnCableConstant {

    public static final Integer HEADERSTARTROW = 1;
    public static final Integer HEADERENDROW = 2;
    public static final Integer STARTCOL = 0;
    public static final Integer ENDCOL = 7;
    public static final Integer CONTENTSTARTROW = 3;
    public static final String CONN = "CONN";
    public static final String CABLE = "CABLE";
    public static final String CONNTYPE = "EDAComPart";
    public static final String CABLETYPE = "D9_CommonPart";
    public static final String TEMPLATEPATH = "/conncable/Conn _Cable_Import_Template.xlsx";
    public static final String TEMPLATESHEETNAME = "Conn_Cable";
    public static final String TEMPLATENAME = "Conn _Cable_Import_Template.xlsx";
    public static final String __WEB_FIND_USER_QUERY_NAME = "__WEB_find_user";
    public static final String[] __WEB_FIND_USER_QUERY_PARAMS = new String[]{"User ID"};


    public static final Map<String, String[]> excelHeaderMap = new HashMap() {{
        put(HEADERSTARTROW, new String[]{"NO", "Conn", "Conn", "Conn", "Cable", "Cable", "Cable"});
        put(HEADERENDROW, new String[]{"NO", "HH PN", "Description", "Supplier", "HH PN", "Description", "Supplier"});
    }};
}
