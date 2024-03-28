package com.foxconn.plm.tcservice.benefitreport.constant;


import java.util.HashMap;
import java.util.Map;

public class ExcelConfig {

    public static final String MNT_TEMPLATE = "/benefitreport/MNT_FTE_20230725_template.xlsx";
    public static final String DT_TEMPLATE = "/benefitreport/DT_FTE_20230725_template.xlsx";
    public static final String FTE2022_DETAIL = "/benefitreport/FTE_2022Detail_template.xlsx";
    public static final int DT_FILL_OFFSET = 3;
    public static final int MNT_FILL_OFFSET = 7;
    //
    public static final int[] DT_START_R_C = new int[]{3, 6};
    public static final int[] DT_TOTAL_CELL = new int[]{30, 12};
    public static final int[] MNT_DATE_CELL = new int[]{1, 16};
    public static final String DT_TEMPLATE2 = "/benefitreport/DT2023.xlsx";
    //
    public static final int[] MNT_START_R_C = new int[]{4, 10};
    public static final int[] MNT_TOTAL_CELL = new int[]{13, 24};
    public static final String MNT_TEMPLATE2 = "/benefitreport/MNT2023.xlsx";
    //
    public static final String MVP2_START_DATE = "2023-06-00";

    public static final Map<String, Integer[]> MNT_BENEFIT_CONFIG = new HashMap<>() {{
        put("P3A0", new Integer[]{3, 39});
        put("P3A", new Integer[]{4, 39});
        put("P3B", new Integer[]{5, 39});
        put("P3C", new Integer[]{6, 39});
        put("P3D", new Integer[]{7, 39});
        put("P3E", new Integer[]{8, 39});
        put("P3F", new Integer[]{9, 39});
        //
        put("P4A0", new Integer[]{3, 40});
        put("P4A", new Integer[]{4, 40});
        put("P4B", new Integer[]{5, 40});
        put("P4C", new Integer[]{6, 40});
        put("P4D", new Integer[]{7, 40});
        put("P4E", new Integer[]{8, 40});
        put("P4F", new Integer[]{9, 40});
        //
        put("P5A0", new Integer[]{3, 41});
        put("P5A", new Integer[]{4, 41});
        put("P5B", new Integer[]{5, 41});
        put("P5C", new Integer[]{6, 41});
        put("P5D", new Integer[]{7, 41});
        put("P5E", new Integer[]{8, 41});
        put("P5F", new Integer[]{9, 41});
        //
        put("P6A0", new Integer[]{3, 42});
        put("P6A", new Integer[]{4, 42});
        put("P6B", new Integer[]{5, 42});
        put("P6C", new Integer[]{6, 42});
        put("P6D", new Integer[]{7, 42});
        put("P6E", new Integer[]{8, 42});
        put("P6F", new Integer[]{9, 42});
        //
        put("P7A0", new Integer[]{3, 43});
        put("P7A", new Integer[]{4, 43});
        put("P7B", new Integer[]{5, 43});
        put("P7C", new Integer[]{6, 43});
        put("P7D", new Integer[]{7, 43});
        put("P7E", new Integer[]{8, 43});
        put("P7F", new Integer[]{9, 43});
    }};

    public static final Map<String, Integer[]> DT_BENEFIT_CONFIG = new HashMap<>() {{
        put("P3E1", new Integer[]{3, 14});
        put("P3E2", new Integer[]{4, 14});
        put("P3E3", new Integer[]{5, 14});
        //
        put("P4E1", new Integer[]{3, 15});
        put("P4E2", new Integer[]{4, 15});
        put("P4E3", new Integer[]{5, 15});
        //
        put("P5E1", new Integer[]{3, 16});
        put("P5E2", new Integer[]{4, 16});
        put("P5E3", new Integer[]{5, 16});
        //
        put("P6E1", new Integer[]{3, 17});
        put("P6E2", new Integer[]{4, 17});
        put("P6E3", new Integer[]{5, 17});
        //
        put("P7E1", new Integer[]{3, 18});
        put("P7E2", new Integer[]{4, 18});
        put("P7E3", new Integer[]{5, 18});
    }};
}
