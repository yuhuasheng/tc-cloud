package com.foxconn.plm.cis.enumconfig;

public class CISConstants {

    // 属性
    public static final String ATTR_OBJECT_TYPE = "object_type";
    public static final String ATTR_OBJECT_NAME = "object_name";
    public static final String ATTR_OBJECT_DESC = "object_desc";
    public static final String ATTR_ITEM_ID = "item_id";
    public static final String PROJECT_IDS = "project_ids";
    public static final String ATTR_D9_PROCUREMENT_METHODS = "d9_ProcurementMethods";
    public static final String ATTR_D9_MATERIAL_TYPE = "d9_MaterialType";
    public static final String ATTR_D9_MATERIAL_GROUP = "d9_MaterialGroup";
    public static final String ATTR_D9_UN = "d9_Un";
    public static final String HHPN = "item_id";
    public static final String MFG = "Part Revision<-items_tag.d9_ManufacturerID";
    public static final String MFG_PN = "Part Revision<-items_tag.d9_ManufacturerPN";
    public static final String D9_EE_PCBARevision = "D9_EE_PCBARevision";

    // 类型
    public static final String TYPE_EDA_COM_PART = "EDAComPart";
    public static final String TYPE_D9_PCB_PART = "D9_PCB_Part";

    // 查询
    public static final String FIND_D9_FIND_FOLDER = "__D9_Find_Folder"; //查询文件夹
    public static final String FIND_PARTS = "__D9_Find_Parts";

    // 流程
    public static final String PROCESS_FXN30_PARTS_BOM_FAST_RELEASE_PROCESS = "FXN30_Parts BOM Fast Release Process";

    // 发布状态
    public static final String STATUS_D9_RELEASE = "D9_Release";

    // 字符串常量
    public static final String STR_FROM_AVL_SYNC = "FROM AVL SYNC";

    public static final String ECAD_BENEFIT_NAME = "ecad_hint.map製作效率";

    public static final String SYMBOL_BENEFIT_NAME = "Symbol共用節省設計時間";

    public static final String FOOTPRINT_PAD_BENEFIT_NAME = "Footprint & PAD共用節省設計時間";
}
