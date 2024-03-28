package com.foxconn.plm.tcservice.ftebenefitreport.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/9/22 9:50
 * @Version 1.0
 */
public class FTEConstant {

    public static final String DT_L5 = "DT_L5";
    public static final String DT_L6 = "DT_L6";
    public static final String DT_L10 = "DT_L10";
    public static final String MNT_L5 = "MNT_L5";
    public static final String MNT_L6 = "MNT_L6";
    public static final String MNT_L10 = "MNT_L10";

    public static final Map<String, Integer> startRowMap = new HashMap() {{
        put(DT_L5, 3);
        put(DT_L6, 1);
        put(DT_L10, 2);
        put(MNT_L5, 3);
        put(MNT_L6, 1);
        put(MNT_L10, 2);
    }};

    public static final Map<String, String[]> excelKeyMap = new HashMap() {{
        put(DT_L5, new String[]{"C"});
        put(DT_L6, new String[]{"B", "C", "D", "F"});
        put(DT_L10, new String[]{"B", "H"});
        put(MNT_L5, new String[]{"C", "E"});
        put(MNT_L6, new String[]{"A", "B", "C"});
        put(MNT_L10, new String[]{"D", "G"});
    }};

    public static final Map<String, String[]> cellSplitMap = new HashMap() {{
        put(DT_L5, new String[]{"M"});
        put(DT_L6, new String[]{"G", "J"});
        put(DT_L10, new String[]{"D"});
        put(MNT_L5, new String[]{"F"});
        put(MNT_L6, new String[]{"D", "F"});
        put(MNT_L10, new String[]{"O"});
    }};

    public static final Map<String, String> dateCellMap = new HashMap() {{
        put(DT_L5, "C");
        put(DT_L6, "F");
        put(DT_L10, "H");
        put(MNT_L5, "E");
        put(MNT_L6, "A");
        put(MNT_L10, "D");
    }};
}
