package com.foxconn.plm.tcreport.drawcountreport.constant;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2023/1/4 11:36
 * @Version 1.0
 */
public class DrawCountReportConstant {

    public static final String D9_PRODUCTNODEREVISION = "D9_ProductNodeRevision";

    public static final String D9_IDNODEREVISION = "D9_IDNodeRevision";

    public static final String D9_CHASSISNODEREVISION = "D9_ChassisNodeRevision";

    public static final String D9_COMPONENTNODEREVISION = "D9_ComponentNodeRevision";

    public static final String D9_SYSTEMNODEREVISION = "D9_SystemNodeRevision";

    public static final String D9_ITEM_CODETYPE = "D9_ITEM_CodeType";

    public static final List<String> ITEMREVTYPELIST = new ArrayList<String>() {{
        add(D9_IDNODEREVISION);
        add(D9_CHASSISNODEREVISION);
        add(D9_COMPONENTNODEREVISION);
    }};

}
