package com.foxconn.plm.utils.tc;



import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.CloseBOMWindowsResponse;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsInfo;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsOutput;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse;
import com.teamcenter.services.strong.cad._2008_06.StructureManagement;
import com.teamcenter.services.strong.cad._2008_06.StructureManagement.SaveBOMWindowsResponse;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.BOMWindow;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructureManagementUtil {

    /**
     * Open BOMWindow
     * @param itemRevision 对象版本
     * @return
     */
    public static List openBOMWindow(StructureManagementService smService, ItemRevision itemRevision) {
        List bomWindowParentLine = new ArrayList(2);
        try {
            CreateBOMWindowsInfo[] createBOMWindowsInfo = new CreateBOMWindowsInfo[1];
            createBOMWindowsInfo[0] = new CreateBOMWindowsInfo();
            createBOMWindowsInfo[0].itemRev = itemRevision;
            createBOMWindowsInfo[0].clientId = "BOMUtils";
            createBOMWindowsInfo[0].item = itemRevision.get_items_tag();
//			createBOMWindowsInfo[0].bomView =
            CreateBOMWindowsResponse createBOMWindowsResponse = smService.createBOMWindows(createBOMWindowsInfo);
            if (createBOMWindowsResponse.serviceData.sizeOfPartialErrors() > 0) {
                for (int i = 0; i < createBOMWindowsResponse.serviceData.sizeOfPartialErrors(); i++) {
                    System.out.println("【ERROR】 Partial Error in Open BOMWindow = "
                            + createBOMWindowsResponse.serviceData.getPartialError(i).getMessages()[0]);
                }
                return null;
            }
            CreateBOMWindowsOutput[] output = createBOMWindowsResponse.output;
            if (null == output || output.length < 0) {
                return null;
            }
            // BOMWindow
            bomWindowParentLine.add(output[0].bomWindow);
            // TOPLine in BOMWindow
            bomWindowParentLine.add(output[0].bomLine);
            return bomWindowParentLine;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(StringUtil.getExceptionMsg(e));
        }
        return null;
    }


    /**
     * Close BOMWindow
     * @param bomWindow  BOM窗口
     */
    public static  void closeBOMWindow(StructureManagementService smService, BOMWindow bomWindow) {
        CloseBOMWindowsResponse response = null;
        if (smService != null && bomWindow != null) {
            response = smService.closeBOMWindows(new BOMWindow[] { bomWindow });
        }
        if (response.serviceData.sizeOfPartialErrors() > 0) {
            for (int i = 0; i < response.serviceData.sizeOfPartialErrors(); i++) {
                System.out.println(
                        "Close BOMWindow Partial Error -- " + response.serviceData.getPartialError(i).getMessages()[0]);
            }
        }
    }


    public static void saveBOMWindow(StructureManagementService smService, BOMWindow bomWindow) {
        SaveBOMWindowsResponse saveResponse = smService.saveBOMWindows(new BOMWindow[] { bomWindow });
        if (saveResponse.serviceData.sizeOfPartialErrors() > 0) {
            for (int i = 0; i < saveResponse.serviceData.sizeOfPartialErrors(); i++) {
                System.out.println("Save BOMWindow Partial Error -- "
                        + saveResponse.serviceData.getPartialError(i).getMessages()[0]);
            }
        }
    }


    /**
     * 判断是否为BOM结构对象
     * @param dmService
     * @param itemRev
     * @return
     * @throws NotLoadedException
     */
    public static  boolean isBom(DataManagementService dmService, ItemRevision itemRev) throws NotLoadedException {
        TCUtils.refreshObject(dmService, itemRev);
        ModelObject[] modelObjects = TCUtils.getPropModelObjectArray(dmService, itemRev, "ps_children");
        if (CollectUtil.isEmpty(modelObjects)) {
            return false;
        }
        return true;
    }

    /**
     * 展开所有的BOMLine，获取所有的BOMLine对象
     * @param smService
     * @param topLine
     * @param sessionService
     * @throws Exception
     */
    public static Map<String, BOMLine> expandPSEAllLevels(StructureManagementService smService, BOMLine topLine, SessionService sessionService) throws Exception {
        Map<String, BOMLine> bomLineMap = new HashMap<String, BOMLine>();
        TCUtils.setDefaultLoadProperty(sessionService, "BOMLine", new String[] {"bl_item_item_id" ,"fnd0bl_is_substitute","bl_revision"});
        com.teamcenter.services.strong.cad._2008_06.StructureManagement.ExpandPSAllLevelsInfo info = new com.teamcenter.services.strong.cad._2008_06.StructureManagement.ExpandPSAllLevelsInfo();
        info.parentBomLines = new BOMLine[]{topLine};
        info.excludeFilter = "None2";
        com.teamcenter.services.strong.cad._2008_06.StructureManagement.ExpandPSAllLevelsPref pref = new com.teamcenter.services.strong.cad._2008_06.StructureManagement.ExpandPSAllLevelsPref();
        StructureManagement.ExpandPSAllLevelsResponse2 response = smService.expandPSAllLevels(info, pref);
        for (int i = 0; i < response.output.length; i++) {
            try {
                BOMLine childline = response.output[i].parent.bomLine;
                bomLineMap.put( childline.get_bl_item_item_id(), childline );
            } catch (NotLoadedException  e) {
                StringBuilder sErrorMessage = new StringBuilder();
                sErrorMessage.append( "Exception occurred while fetching the item id for loaded structure. " );
                throw new Exception( sErrorMessage.toString() );
            }
        }
        return bomLineMap;
    }

}
