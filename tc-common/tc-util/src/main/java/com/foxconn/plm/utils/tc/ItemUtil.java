package com.foxconn.plm.utils.tc;

import com.foxconn.plm.entity.constants.TCItemConstant;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2023/3/10 15:15
 * @Version 1.0
 */
public class ItemUtil {


    /**
     * 创建零组件
     * @param dmService
     * @param propMaps
     * @param folder
     * @return
     */
    public static com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsResponse createItems(DataManagementService dmService, List<Map<String, String>> propMaps, ModelObject folder, String relationName) {
        com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[] itemProps = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[propMaps.size()];
        for (int i = 0; i < propMaps.size(); i++) {
            Map<String, String> propMap = propMaps.get(i);
            com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties itemProperty = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties();
            Iterator<Map.Entry <String, String >> it = propMap.entrySet().iterator();
            String type = null;
            while (it.hasNext()) {
                Map.Entry <String, String > entry = it.next();
                String propName = entry.getKey();
                String value = entry.getValue();
                if (TCItemConstant.PROPERTY_ITEM_ID.equals(propName)) {
                    itemProperty.itemId = value;
                    it.remove();
                } else if (TCItemConstant.PROPETY_ITEM_REVISION_ID.equals(propName)) {
                    itemProperty.revId = value;
                    it.remove();
                } else if (TCItemConstant.PROPERTY_OBJECT_NAME.equals(propName)) {
                    itemProperty.name = value;
                    it.remove();
                } else if (TCItemConstant.PROPERTY_OBJECT_TYPE.equals(propName)) {
                    itemProperty.type = value;
                    type = value;
                    it.remove();
                } else if (TCItemConstant.PROPERTY_OBJECT_DESC.equals(propName)) {
                    itemProperty.description = value;
                    it.remove();
                }
            }

            itemProperty.clientId = "AppX-Test";
            itemProps[i] = itemProperty;

            if (propMap != null && propMap.size() > 0) {
                itemProperty.extendedAttributes = new DataManagement.ExtendedAttributes[1];
                DataManagement.ExtendedAttributes theExtendedAttr = new DataManagement.ExtendedAttributes();
                theExtendedAttr.attributes = propMap;
                theExtendedAttr.objectType = type;
                itemProperty.extendedAttributes[0] = theExtendedAttr;
                itemProps[0] = itemProperty;
            }

        }
        return dmService.createItems(itemProps, folder, relationName);
    }


    public static   com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsResponse createItems(DataManagementService dmService, List<Map<String, String>> propMaps, ModelObject folder) {
        com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[] itemProps = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[propMaps.size()];
        for (int i = 0; i < propMaps.size(); i++) {
            Map<String, String> propMap = propMaps.get(i);
            com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties itemProperty = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties();
            itemProperty.itemId = propMap.get("item_id");
            if (propMap.containsKey("item_revision_id")) {
                itemProperty.revId = propMap.get("item_revision_id");
            }
            itemProperty.name = propMap.get("object_name");
            itemProperty.type = propMap.get("object_type");
            itemProperty.description = propMap.getOrDefault("object_desc", "");
            itemProperty.clientId = "AppX-Test";
            itemProps[i] = itemProperty;
        }
        return dmService.createItems(itemProps, folder, "");
    }

    // 获取最新版本
    public static ItemRevision getItemLatestRevision(DataManagementService dmService, Item item) {
        try {
            ModelObject[] objects = {item};
            String[] atts = {"revision_list"};
            dmService.getProperties(objects, atts);
            ModelObject[] itemRevs = item.get_revision_list();
            ItemRevision itemRev = (ItemRevision) itemRevs[itemRevs.length - 1];
            return itemRev;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
