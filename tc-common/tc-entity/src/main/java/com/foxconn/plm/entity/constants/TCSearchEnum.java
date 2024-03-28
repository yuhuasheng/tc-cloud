package com.foxconn.plm.entity.constants;

/**
* @author infodba
* @version 创建时间：2021年12月23日 下午7:28:43
* @Description TC查询条件枚举类
*/
public enum TCSearchEnum {

    D9_Find_Schedule_Task("__D9_Find_Schedule_Task",new String[]{"startDate","endDate"}),
    WEB_FIND_USER("__WEB_find_user", new String[] {"User ID"}),
    D9_Find_ProductNode("__D9_Find_ProductNode", new String[] {"Project ID"}),
    D9_Find_Running_Project("__D9_Find_Project_Running", new String[] {"Project ID"}),
    D9_FIND_PROJECT_FOLDER("__D9_Find_Project_Folder", new String[] {"SPAS ID", "Name"}),
    D9_FIND_PROJECT("__D9_Find_Project", new String[] {"project_id"}),
    ITEM_NAME_OR_ID("Item_Name_or_ID", new String[]{"item_id"}),
    D9_FIND_ACTUALUSER("__D9_Find_Actual_User", new String[]{"ID"}),
    D9_FIND_PACPARTREV("__D9_Find_PACPartRev_ByProjectID", new String[]{"project_list.project_id"}),
    D9_FIND_MATERIALGROUP("__D9_Find_MaterialGroup", new String[] {"structure_revisions.PSOccurrence<-parent_bvr.Part:child_item.item_id"});

    private final String queryName; // 查询名称

	private final String[] queryParams; // 查询参数名
    private TCSearchEnum(String queryName, String[] queryParams) {
    	this.queryName = queryName;
        this.queryParams = queryParams;
    }

    public String queryName() {
    	return queryName;
	}
    
    public String[] queryParams() {
        return queryParams;
    }
}
