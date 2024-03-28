package com.foxconn.plm.integrate.spas.scheduled;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCItemConstant;
import com.foxconn.plm.entity.constants.TCPreferenceConstant;
import com.foxconn.plm.entity.constants.TCSearchEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.integrate.spas.domain.SPASUser;
import com.foxconn.plm.integrate.spas.mapper.SpasMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.date.DateUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.PreferencesUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Folder;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @Author MW00333
 * @Date 2023/5/19 8:49
 * @Version 1.0
 */
@Component
public class SpasUserSyncTask {

    private static Log log = LogFactory.get();

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    @Resource
    private SpasMapper spasMapper;

    @XxlJob("spasUserSyncScheduling")
   // @PostConstruct
    public void handlerData() {
        log.info("==>> 开始同步SPAS用户信息");
        XxlJobHelper.log("==>> 开始同步SPAS用户信息");
        TCSOAServiceFactory tcsoaServiceFactory = null;
        try {
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
            PreferenceManagementService pfService = tcsoaServiceFactory.getPreferenceManagementService();

            String[]  prefixs=PreferencesUtil.getTCPreferences(pfService,"D9_SPAS_WorkID_Prefix");
            String  prefix=prefixs[0];

            List<SPASUser> spasUserList = spasMapper.getSpasUserInfoByDate(null, null,prefix);
            if (CollectUtil.isEmpty(spasUserList)) {
                XxlJobHelper.log("未查询到需要同步SPAS用户信息的数据");
                return;
            }

            Collections.sort(spasUserList); // 进行排序
            spasUserList = spasUserList.stream().filter(CollectUtil.distinctByKey(SPASUser::getWorkId)).collect(Collectors.toList());


            SavedQueryService savedQueryService = tcsoaServiceFactory.getSavedQueryService();
            DataManagementService dmService = tcsoaServiceFactory.getDataManagementService();

            List<SPASUser> list = new CopyOnWriteArrayList<>();
            spasUserList.stream().parallel().forEach(spasUser -> {
                System.out.println(spasUser.toString());
                String workId = spasUser.getWorkId();
                try {
                    ModelObject[] objects = TCUtils.executequery(savedQueryService, dmService, TCSearchEnum.D9_FIND_ACTUALUSER.queryName(), TCSearchEnum.D9_FIND_ACTUALUSER.queryParams(), new String[]{workId});
                    if (CollectUtil.isEmpty(objects)) {
                        if("1".equalsIgnoreCase(spasUser.getIsActive())) {
                            list.add(spasUser);
                        }
                    } else {
                        updateActualUserInfo(dmService, objects[0], spasUser);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getLocalizedMessage());
                }
            });


            if (CollectUtil.isNotEmpty(list)) {
                log.info("running create items  -->>>");
                createActualUserItem(pfService, dmService, list);
                log.info("end create items  -->>>");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新实际用户信息
     * @param dmService
     * @param obj
     * @param user
     */
    private void updateActualUserInfo(DataManagementService dmService, ModelObject obj, SPASUser user) {
            Map<String, String> propMap = getTCPropMap(user);

            TCUtils.setProperties(dmService, obj, propMap);

    }


    private void createActualUserItem(PreferenceManagementService pfService, DataManagementService dmService, List<SPASUser> list) throws Exception {
        String[] tcPreferences = TCUtils.getTCPreferences(pfService, TCPreferenceConstant.D9_SPAS_USER_FOLDER_UID);
        String uid = tcPreferences[0];
        Folder SPASActiveUserFolder = TCUtils.findFolderByUid(dmService, uid);
        List<Map<String, String>> newMapList = list.stream().map(e -> {
            Map<String, String> propMap = getTCPropMap(e);
            return propMap;
        }).collect(Collectors.toList());

        List<List<Map<String, String>>> groupList = CollectUtil.fixedGrouping(newMapList, 20);
        groupList.forEach(obj -> {
            DataManagement.CreateItemsResponse response = createItems(dmService, obj, SPASActiveUserFolder);
            int errorSize = response.serviceData.sizeOfPartialErrors();
            if (errorSize > 0) {
                log.error("【ERROR】create item  response error size : " + errorSize);
                for (int i = 0; i < errorSize; i++) {
                    log.error("【ERROR】create item response error info : " + Arrays.toString(response.serviceData.getPartialError(i).getMessages()));
                }
            }
        });
    }

    /**
     * 获取TC属性
     * @param user
     * @return
     */
    private Map<String, String> getTCPropMap(SPASUser user) {
        log.info("start -->>  getTCPropMap");
        Map<String, String> tcPropMap = new HashMap<>();
        tcPropMap.put(TCItemConstant.PROPERTY_ITEM_ID, StringUtil.replaceBlank(user.getWorkId()));
        tcPropMap.put(TCItemConstant.PROPERTY_OBJECT_NAME, StringUtil.replaceBlank(user.getName()));
        tcPropMap.put(TCItemConstant.PROPERTY_OBJECT_TYPE, "D9_ActualUser");
        String isActive = StringUtil.replaceBlank(user.getIsActive());
        if ("0".equals(isActive)) {
            tcPropMap.put(TCItemConstant.PROPERTY_OBJECT_DESC, "N");
        } else if ("1".equals(isActive)) {
            tcPropMap.put(TCItemConstant.PROPERTY_OBJECT_DESC, "Y");
        }
        tcPropMap.put(TCItemConstant.PROPERTY_D9_EMAIL, user.getNotes());
        log.info("tcPropMap :: " + tcPropMap);
        return tcPropMap;
    }



    private  com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsResponse createItems(DataManagementService dmService, List<Map<String, String>> propMaps, ModelObject folder) {
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
            com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes[] attrs= new com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes[1];
            com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes attr=new com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes();
            Map<String, String> attMap=new HashMap<>();
            attMap.put(TCItemConstant.PROPERTY_D9_EMAIL,propMap.get("propMap"));
            attr.attributes=attMap;
            attrs[0]=attr;
            itemProperty.extendedAttributes=attrs;
            itemProps[i] = itemProperty;
        }
        return dmService.createItems(itemProps, folder, "");
    }

}
