package com.foxconn.plm.tcservice.dcnreport.constant;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/10/22 14:07
 * @Version 1.0
 */
public class DCNReportConstant {

    public final static String SHEET_METAL = "ME-SM";

    public final static String PLASTIC = "ME-PL";

    public static final String DTTEMPLATEPATH = "/dcnreport/DT_DCN_Report_Template.xlsx";

    public static final String MNTTEMPLATEPATH = "/dcnreport/MNT_DCN_Report_Template.xlsx";

    public static final String CONFIGPATH = "/dcnreport/config.properties";

    public static final String TEMPLATEFILE = "DCN_Report_Template.xlsx";

    public static final String SHEET_METAL_SHEET_NAME = "Sheet Metal DCN匯總";

    public static final String PLASTIC_SHEET_NAME = "Plastic DCN匯總";

    public static final String SUMMARY_SHEET_NAME = "Summary";

    public static final String DCNCOSTCOUNT_SHEET_NAME = "DCN費用統計";

    public static final String DCNCOSTCOUNT_SHEET_HEADER_NAME = "專案DCN費用統計";

    public static final String DCNCOSTCOUNT_SHEET_DATE_TEMEPLATE = "X年X月X日~X年X月X日";

    public static final int DCNCOSTCOUNT_SHEET_HEADSTARTROW = 1;

    public static final int DCNCOSTCOUNT_SHEET_CONTENTSTARTROW = 8;

    public static final String DCNCOSTCOUNT_SHEET_STARTCOL = "B";

    public static final int DCNCOSTCOUNT_SHEET_TABLEHEADERROW = 6;

    public static final String DCNCOSTCOUNT_SHEET_HEADERCOL = "E";

    public static final int DCNCOSTCOUNT_SHEET_ENDROW = 28;

    public static final int SHEET_METAL_SHEET_STARTROW = 2;

    public static final int SHEET_METAL_SHEET_COLLENGTH = 14;

    public static final int PLASTIC_SHEET_STARTROW = 2;

    public static final int PLASTIC_SHEET_COLLENGTH = 14;

    private static String custRequestProp = "d9_CustomerRequest";
    private static String MEImproveProp = "d9_DesignImprovement_ME";
    private static String otherImproveProp = "d9_DesignImprovement_Others";
    private static String processImproveProp = "d9_ProcessImprovement";
    private static String DFXImproveProp = "d9_DFXImprovement";

    private static String custRequestDisplayName = "客戶需求";
    private static String MEImproveDisplayName = "設計改善---機構";
    private static String otherImproveDisplayName = "設計改善---其他";
    private static String processImproveDisplayName = "製程改善";
    private static String DFXImproveDisplayName = "DFX類改善";
    public static String POJECTCOUNT = "專案數量";
    public static String DCNNUMBER = "DCN次數";
    public static String DCNCOSTIMPACT = "DCN費用（RMB）";
    public static String REASONFLAG = "費用未填寫";
    public static String DCNALLRELEASEFLAG = "";
    public static String DCNNOTRELEASEFLAG = "0";
    public static String DCNRELEASEFLAG = "1";

    public static String DCNALLCOSTIMPACTFLAG = "";
    public static String DCNNOTCOSTIMPACTFLAG = "0";
    public static String DCNCOSTIMPACTFLAG = "1";

    public static String D9_MEDESIGNREVISION = "D9_MEDesignRevision";

    public static final Map<String, String> REASONMAP = new HashMap() {{
        put(custRequestProp, custRequestDisplayName);
        put(MEImproveProp, MEImproveDisplayName);
        put(otherImproveProp, otherImproveDisplayName);
        put(processImproveProp, processImproveDisplayName);
        put(DFXImproveProp, DFXImproveDisplayName);
    }};

    public static final Map<String, String> DTDCNCostCountMap = new LinkedHashMap() {{
        put("DT,ME-SM,客戶需求,DCN次數", "9,E");
        put("DT,ME-SM,設計改善---機構,DCN次數", "10,E");
        put("DT,ME-SM,設計改善---其他,DCN次數", "11,E");
        put("DT,ME-SM,製程改善,DCN次數", "12,E");
        put("DT,ME-SM,DFX類改善,DCN次數", "13,E");
        put("DT,ME-SM,客戶需求,DCN費用（RMB）", "9,F");
        put("DT,ME-SM,設計改善---機構,DCN費用（RMB）", "10,F");
        put("DT,ME-SM,設計改善---其他,DCN費用（RMB）", "11,F");
        put("DT,ME-SM,製程改善,DCN費用（RMB）", "12,F");
        put("DT,ME-SM,DFX類改善,DCN費用（RMB）", "13,F");
        put("DT,ME-PL,客戶需求,DCN次數", "16,E");
        put("DT,ME-PL,設計改善---機構,DCN次數", "17,E");
        put("DT,ME-PL,設計改善---其他,DCN次數", "18,E");
        put("DT,ME-PL,製程改善,DCN次數", "19,E");
        put("DT,ME-PL,DFX類改善,DCN次數", "20,E");
        put("DT,ME-PL,客戶需求,DCN費用（RMB）", "16,F");
        put("DT,ME-PL,設計改善---機構,DCN費用（RMB）", "17,F");
        put("DT,ME-PL,設計改善---其他,DCN費用（RMB）", "18,F");
        put("DT,ME-PL,製程改善,DCN費用（RMB）", "19,F");
        put("DT,ME-PL,DFX類改善,DCN費用（RMB）", "20,F");
    }};

    public static final Map<String, String> MNTDCNCostCountMap = new LinkedHashMap() {{
        put("MNT,ME-SM,客戶需求,DCN次數", "9,E");
        put("MNT,ME-SM,設計改善---機構,DCN次數", "10,E");
        put("MNT,ME-SM,設計改善---其他,DCN次數", "11,E");
        put("MNT,ME-SM,製程改善,DCN次數", "12,E");
        put("MNT,ME-SM,DFX類改善,DCN次數", "13,E");
        put("MNT,ME-SM,客戶需求,DCN費用（RMB）", "9,F");
        put("MNT,ME-SM,設計改善---機構,DCN費用（RMB）", "10,F");
        put("MNT,ME-SM,設計改善---其他,DCN費用（RMB）", "11,F");
        put("MNT,ME-SM,製程改善,DCN費用（RMB）", "12,F");
        put("MNT,ME-SM,DFX類改善,DCN費用（RMB）", "13,F");
        put("MNT,ME-PL,客戶需求,DCN次數", "16,E");
        put("MNT,ME-PL,設計改善---機構,DCN次數", "17,E");
        put("MNT,ME-PL,設計改善---其他,DCN次數", "18,E");
        put("MNT,ME-PL,製程改善,DCN次數", "19,E");
        put("MNT,ME-PL,DFX類改善,DCN次數", "20,E");
        put("MNT,ME-PL,客戶需求,DCN費用（RMB）", "16,F");
        put("MNT,ME-PL,設計改善---機構,DCN費用（RMB）", "17,F");
        put("MNT,ME-PL,設計改善---其他,DCN費用（RMB）", "18,F");
        put("MNT,ME-PL,製程改善,DCN費用（RMB）", "19,F");
        put("MNT,ME-PL,DFX類改善,DCN費用（RMB）", "20,F");
    }};
}
