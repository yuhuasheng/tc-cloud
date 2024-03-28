package com.foxconn.plm.tcreport.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.foxconn.plm.entity.constants.TCBOMLineConstant;
import com.foxconn.plm.entity.constants.TCDatasetEnum;
import com.foxconn.plm.entity.constants.TCItemConstant;
import com.foxconn.plm.entity.constants.TCWorkflowStatusEnum;
import com.foxconn.plm.tcreport.drawcountreport.domain.DrawCountBean;
import com.foxconn.plm.tcreport.drawcountreport.domain.DrawCountEntity;
import com.foxconn.plm.tcreport.drawcountreport.domain.DrawCountRes;
import com.foxconn.plm.utils.tc.StructureManagementUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.structuremanagement.StructureService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.BOMWindow;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static com.foxconn.plm.tcreport.drawcountreport.constant.DrawCountReportConstant.*;

/**
 * @ClassName: FunctionUtil
 * @Description:
 * @Author DY
 * @Create 2023/4/27
 */
public class FunctionUtil {

    public static List<DrawCountEntity> transToEntity(List<DrawCountBean> totalList) {
        Collections.sort(totalList);
        Map<String, DrawCountRes> resMap = new LinkedHashMap<>(totalList.size());
        for (DrawCountBean bean : totalList) {
            String key = bean.getBu() + bean.getCustomer() + bean.getProductLine() + bean.getProjectSeries() + bean.getProjectName()
                    + bean.getDesignTreeName() + bean.getDesignTreeType() + bean.getItemCode() + bean.getItemName() + bean.getItemType();
            DrawCountRes res = resMap.get(key);
            if (ObjectUtil.isNull(res)) {
                res = new DrawCountRes(bean);
                resMap.put(key, res);
            }
            // 處理基礎數據
            res.setUploadNum(res.getUploadNum() + bean.getUploadNum());
            res.setReleaseModelNum(res.getReleaseModelNum() + bean.getReleaseModelNum());
            res.setReleaseNum(res.getReleaseNum() + bean.getReleaseNum());
        }
        int row = 0;
        double total = 0D;
        for (DrawCountRes bean : resMap.values()) {
            if (bean.getUploadNum() == 0) {
                bean.setReleaseProgress("0%");
            } else {
                String s = NumberUtil.roundStr(NumberUtil.div(bean.getReleaseNum() * 100, bean.getUploadNum().intValue()), 1);
                bean.setReleaseProgress(s + "%");
            }
            double value = 0D;
            if (bean.getReleaseNum() == 0) {
                bean.setItemCompleteness("0%");
                value = 0;
            } else {
                value = NumberUtil.div(bean.getReleaseModelNum() * 100, bean.getReleaseNum().intValue());
                String s = NumberUtil.roundStr(value, 1);
                bean.setItemCompleteness(s + "%");
            }
            row += 1;
            total += value / 100;
        }
        List<DrawCountEntity> resList = new ArrayList<>();
        for (DrawCountRes bean : resMap.values()) {
            if (total == 0D) {
                bean.setDrawCompleteness("0%");
            } else {
                String s = NumberUtil.roundStr(NumberUtil.div(total * 100, row), 1);
                bean.setDrawCompleteness(s + "%");
            }
            DrawCountEntity entity = new DrawCountEntity();
            BeanUtil.copyProperties(bean, entity);
            resList.add(entity);
        }
        return resList;
    }

    public static DrawCountBean mntCount(PreferenceManagementService pmService, StructureManagementService smService,
                                         StructureService strucService, DataManagementService dmService, ModelObject obj) {
        BOMWindow[] bomWindows = null; // BOMWindow窗口
        try {
            List createBOMWindowsResponse = StructureManagementUtil.openBOMWindow(smService, (ItemRevision) obj);
            if (createBOMWindowsResponse == null || createBOMWindowsResponse.size() <= 0) {
                throw new Exception("【ERROR】 打开BOMWindow失败！");
            }

            bomWindows = new BOMWindow[]{(BOMWindow) createBOMWindowsResponse.get(0)};
            BOMLine topLine = (BOMLine) createBOMWindowsResponse.get(1);
            // 判断是否打包
            Boolean isPacked = TCUtils.getPropBoolean(dmService, topLine, TCBOMLineConstant.PROPERTY_BL_IS_PACKED);
            if (isPacked) {
                System.out.println("BOM Line is packed, unpacking the complete BOMLine");
                ServiceData response = strucService.packOrUnpack(new BOMLine[]{topLine}, 3);
                if (response.sizeOfPartialErrors() == 0) {
                    System.out.println("unpacking successfull ");
                }
            }
            DrawCountBean drawCountBean = new DrawCountBean();
            setUser(drawCountBean, dmService, (ItemRevision) obj);
            return setMeDrawAndModelCount(pmService, dmService, topLine, drawCountBean, false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bomWindows != null) { // 关闭BOMWindow
                StructureManagementUtil.closeBOMWindow(smService, bomWindows[0]);
            }
        }
        return null;
    }

    /**
     * 设置所有者、所在组、实际工作者
     */
    public static void setUser(DrawCountBean bean, DataManagementService dmService, ModelObject modelObject) throws NotLoadedException {
        TCUtils.getProperty(dmService, new ModelObject[]{modelObject}, new String[]{"owning_user", "owning_group", "d9_ActualUserID"});
        TCUtils.refreshObject(dmService, modelObject);
        ModelObject owning_user = modelObject.getPropertyObject("owning_user").getModelObjectValue();
        TCUtils.getProperty(dmService, new ModelObject[]{owning_user}, new String[]{"user_name", "user_id"});
        TCUtils.refreshObject(dmService, owning_user);
        String userName = owning_user.getPropertyObject("user_name").getStringValue();
        String userId = owning_user.getPropertyObject("user_id").getStringValue();
        ModelObject owning_group = modelObject.getPropertyObject("owning_group").getModelObjectValue();
        TCUtils.getProperty(dmService, owning_group, "full_name");
        TCUtils.refreshObject(dmService, owning_group);
        String group = owning_group.getPropertyObject("full_name").getStringValue();
        String d9_ActualUserID = modelObject.getPropertyObject("d9_ActualUserID").getStringValue();
        bean.setOwner(userName + "(" + userId + ")");
        bean.setOwnerGroup(group);
        bean.setPractitioner(d9_ActualUserID);
    }

    public static List<DrawCountBean> sendPSE(PreferenceManagementService pmService, StructureManagementService smService,
                                              StructureService strucService, DataManagementService dmService, ModelObject obj) {
        BOMWindow[] bomWindows = null; // BOMWindow窗口
        try {
            List createBOMWindowsResponse = StructureManagementUtil.openBOMWindow(smService, (ItemRevision) obj); // Open BOMWindow
            if (createBOMWindowsResponse == null || createBOMWindowsResponse.size() <= 0) {
                throw new Exception("【ERROR】 打开BOMWindow失败！");
            }

            bomWindows = new BOMWindow[]{(BOMWindow) createBOMWindowsResponse.get(0)}; // BOMWindow窗口
            BOMLine topLine = (BOMLine) createBOMWindowsResponse.get(1); // 顶层BOMLine

            ItemRevision itemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, topLine, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);
            String objectType = itemRev.getTypeObject().getName();
            if (!D9_PRODUCTNODEREVISION.equals(objectType)) {
                return null;
            }

            Boolean isPacked = TCUtils.getPropBoolean(dmService, topLine, TCBOMLineConstant.PROPERTY_BL_IS_PACKED); // 判断是否打包
            if (isPacked) {
                System.out.println("BOM Line is packed, unpacking the complete BOMLine");
                ServiceData response = strucService.packOrUnpack(new BOMLine[]{topLine}, 3);
                if (response.sizeOfPartialErrors() == 0) {
                    System.out.println("unpacking successfull ");
                }
            }

            return traverseBOMTree(pmService, dmService, topLine);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bomWindows != null) { // 关闭BOMWindow
                StructureManagementUtil.closeBOMWindow(smService, bomWindows[0]);
            }
        }
        return null;
    }

    private static List<DrawCountBean> traverseBOMTree(PreferenceManagementService pmService, DataManagementService dmService, BOMLine topLine) throws NotLoadedException {
        List<DrawCountBean> list = new CopyOnWriteArrayList<>();
        ModelObject[] children = TCUtils.getPropModelObjectArray(dmService, topLine, TCBOMLineConstant.PROPERTY_BL_ALL_CHILD_LINES);
        if (children == null || children.length <= 0) {
            return null;
        }

        Stream.of(children).parallel().forEach(obj -> {
            try {
                BOMLine childBomLine = (BOMLine) obj;
                ItemRevision itemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, childBomLine, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);
                String objectType = itemRev.getTypeObject().getName();
                if (!itemRevTypeAnyMatch(objectType)) {
                    return;
                }

                DrawCountBean bean = new DrawCountBean();
                bean.setDesignTreeType(TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_OBJECT_NAME));
                setUser(bean, dmService, itemRev);

                ModelObject[] children1 = TCUtils.getPropModelObjectArray(dmService, childBomLine, TCBOMLineConstant.PROPERTY_BL_ALL_CHILD_LINES);
                if (children1 == null || children1.length <= 0) {
                    if (D9_IDNODEREVISION.equalsIgnoreCase(objectType) || D9_CHASSISNODEREVISION.equalsIgnoreCase(objectType)) {
                        bean.setDesignTreeName(bean.getDesignTreeType());
                        DrawCountBean itemBean = (DrawCountBean) bean.clone();
                        itemBean.setUploadNum(1);
                        list.add(itemBean);
                    }
                    return;
                }

                if (D9_IDNODEREVISION.equalsIgnoreCase(objectType) || D9_CHASSISNODEREVISION.equalsIgnoreCase(objectType)) {
                    bean.setDesignTreeName(bean.getDesignTreeType());
                    if (D9_IDNODEREVISION.equalsIgnoreCase(objectType)) {
                        // id分支，统计id分支下的所有节点
                        Stream.of(children1).parallel().forEach(childObj -> {
                            try {
                                DrawCountBean itemBean = (DrawCountBean) bean.clone();
                                itemBean = setDrawAndModelCount(pmService, dmService, childObj, itemBean);
                                if (itemBean != null) {
                                    if (itemBean.getUploadNum() == 0) {
                                        itemBean.setUploadNum(1);
                                    }
                                    list.add(itemBean);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } else {
                        // 系统分支，只统计ME-AS01开头的这个料的数据
                        for (ModelObject object : children1) {
                            String str = TCUtils.getPropStr(dmService, object, TCBOMLineConstant.PROPERTY_BL_ITEM_ITEM_ID);
                            if (!str.startsWith("ME-AS01-")) {
                                continue;
                            }
                            try {
                                DrawCountBean itemBean = (DrawCountBean) bean.clone();
                                itemBean = setMeDrawAndModelCount(pmService, dmService, object, itemBean, true);
                                if (itemBean != null) {
                                    if (itemBean.getUploadNum() == 0) {
                                        itemBean.setUploadNum(1);
                                    }
                                    list.add(itemBean);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else if (D9_COMPONENTNODEREVISION.equalsIgnoreCase(objectType)) {
                    List<ModelObject> systemNodes = filterSystemNodeType(dmService, children1);
                    systemNodes.parallelStream().forEach(systemObj -> {
                        try {
                            BOMLine systemBomLine = (BOMLine) systemObj;
                            ItemRevision systemItemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, systemBomLine, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);

                            DrawCountBean systemBean = (DrawCountBean) bean.clone();
                            systemBean.setDesignTreeName(TCUtils.getPropStr(dmService, systemItemRev, TCItemConstant.PROPERTY_OBJECT_NAME));
                            setUser(systemBean, dmService, systemItemRev);
                            ModelObject[] children2 = TCUtils.getPropModelObjectArray(dmService, systemBomLine, TCBOMLineConstant.PROPERTY_BL_ALL_CHILD_LINES);
                            if (children2 == null || children2.length <= 0) {
                                if (systemBean.getUploadNum() == 0) {
                                    systemBean.setUploadNum(1);
                                }
                                list.add(systemBean);
                                return;
                            }

                            Stream.of(children2).parallel().forEach(systemChildObj -> {
                                try {
                                    DrawCountBean systemChildBean = (DrawCountBean) systemBean.clone();
                                    systemChildBean = setDrawAndModelCount(pmService, dmService, systemChildObj, systemChildBean);
                                    if (systemChildBean != null) {
                                        list.add(systemChildBean);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return list;
    }

    private static DrawCountBean setMeDrawAndModelCount(PreferenceManagementService pmService, DataManagementService dmService, ModelObject obj, DrawCountBean bean, boolean filter) throws Exception {
        BOMLine bomLine = (BOMLine) obj;
        ItemRevision itemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, bomLine, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);
        DrawCountBean newBean = (DrawCountBean) bean.clone();
        newBean = setItemParams(pmService, dmService, itemRev, newBean);
        // 递归计算所有料
        Map<String, ItemRevision> itemRevisionMap = new HashMap<>(64);
        if (!filter) {
            String str = TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_ITEM_ID);
            if (str.startsWith("ME-")) {
                itemRevisionMap.put(str, itemRev);
            }
        }
        getAllItemRevision(dmService, bomLine, itemRevisionMap, filter);
        newBean.setUploadNum(itemRevisionMap.keySet().size());
        for (ItemRevision itemRevision : itemRevisionMap.values()) {
            if (TCUtils.isReleased(dmService, itemRevision, new String[]{
                    TCWorkflowStatusEnum.D9_FastRelease.name(), TCWorkflowStatusEnum.D9_Release.name(), TCWorkflowStatusEnum.TCMReleased.name(), TCWorkflowStatusEnum.Released.name()})) {
                newBean.setReleaseNum(newBean.getReleaseNum() + 1);
                int modelNum = getModelNum(dmService, itemRev);
                newBean.setReleaseModelNum(modelNum + newBean.getReleaseModelNum());
            }
        }
        return newBean;
    }

    private static void getAllItemRevision(DataManagementService dmService, BOMLine bomLine, Map<String, ItemRevision> map, boolean filter) throws Exception {
        ModelObject[] childrenList = TCUtils.getPropModelObjectArray(dmService, bomLine, TCBOMLineConstant.PROPERTY_BL_ALL_CHILD_LINES);
        if (childrenList != null && childrenList.length > 0) {
            for (ModelObject child : childrenList) {
                ItemRevision childItemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, child, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);
                String str = TCUtils.getPropStr(dmService, childItemRev, TCItemConstant.PROPERTY_ITEM_ID);
                if (filter && StrUtil.isNotBlank(str) && str.startsWith("ME-") && !str.startsWith("ME-AS01-") && !str.startsWith("ME-AS02-") &&
                        !str.startsWith("ME-AS03-") && !str.startsWith("ME-PTTP-") && !str.startsWith("ME-SKEL-") && !str.startsWith("ME-PTDD-")
                        && !str.startsWith("ME-ASDD-") && !str.startsWith("ME-ASTP-") && !str.startsWith("ME-PTFM-") && ObjectUtil.isNull(map.get(str))) {
                    map.put(str, childItemRev);
                } else if (!filter && StrUtil.isNotBlank(str) && str.startsWith("ME-") && ObjectUtil.isNull(map.get(str))) {
                    map.put(str, childItemRev);
                }
                getAllItemRevision(dmService, (BOMLine) child, map, filter);
            }
        }
    }

    /**
     * 设置零件参数
     *
     * @param pmService 首选项工具类
     * @param dmService 工具类
     * @param bean      实体类
     * @return
     * @throws NotLoadedException
     */
    private static DrawCountBean setItemParams(PreferenceManagementService pmService, DataManagementService dmService, ItemRevision itemRev, DrawCountBean bean) throws Exception {
        String itemId = TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_ITEM_ID);
        bean.setItemCode(itemId);
        bean.setItemName(TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_OBJECT_NAME));
        String itemType = itemTypeMatch(pmService, itemId);
        if (StrUtil.isNotBlank(itemType)) {
            bean.setItemType(itemType);
        }
        return bean;
    }


    private static DrawCountBean setDrawAndModelCount(PreferenceManagementService pmService, DataManagementService dmService, ModelObject obj, DrawCountBean bean) throws Exception {
        BOMLine bomLine = (BOMLine) obj;
        ItemRevision itemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, bomLine, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);
        String str = TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_ITEM_ID);
        if (StrUtil.isBlank(str) || str.startsWith("EXT-")) {
            return null;
        }
        DrawCountBean newBean = (DrawCountBean) bean.clone();
        newBean = setItemParams(pmService, dmService, itemRev, newBean);
        if (newBean == null) {
            return null;
        }
        // 递归统计数量
        countNum(dmService, bomLine, newBean);
        return newBean;
    }

    private static void countNum(DataManagementService dmService, BOMLine bomLine, DrawCountBean bean) throws NotLoadedException {
        ItemRevision itemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, bomLine, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);
        bean.setUploadNum(bean.getUploadNum() + 1);
        if (TCUtils.isReleased(dmService, itemRev, new String[]{
                TCWorkflowStatusEnum.D9_FastRelease.name(), TCWorkflowStatusEnum.D9_Release.name(), TCWorkflowStatusEnum.TCMReleased.name(), TCWorkflowStatusEnum.Released.name()})) {
            bean.setReleaseNum(bean.getReleaseNum() + 1);
            int modelNum = getModelNum(dmService, itemRev);
            bean.setReleaseModelNum(modelNum + bean.getReleaseModelNum());
        }
        ModelObject[] children = TCUtils.getPropModelObjectArray(dmService, bomLine, TCBOMLineConstant.PROPERTY_BL_ALL_CHILD_LINES);
        if (children != null && children.length > 0) {
            for (ModelObject child : children) {
                ItemRevision childItemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, child, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);
                String str = TCUtils.getPropStr(dmService, childItemRev, TCItemConstant.PROPERTY_ITEM_ID);
                if (StrUtil.isBlank(str) || str.startsWith("EXT-")) {
                    return;
                }
                countNum(dmService, (BOMLine) child, bean);
            }
        }
    }


    /**
     * 过滤掉不为D9_SystemNodeRevision
     *
     * @param dmService
     * @param objs
     * @return
     */
    private static List<ModelObject> filterSystemNodeType(DataManagementService dmService, ModelObject[] objs) {
        List<ModelObject> list = new ArrayList<>(Arrays.asList(objs));
        list.removeIf(obj -> {
            try {
                BOMLine bomLine = (BOMLine) obj;
                ItemRevision itemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, bomLine, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);
                String objectType = itemRev.getTypeObject().getName();
                if (!objectType.equalsIgnoreCase(D9_SYSTEMNODEREVISION)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
        return list;
    }

    /**
     * 返回零件类别
     *
     * @param pmService 首选项工具类
     * @param itemId    零组件ID
     * @return
     */
    public static String itemTypeMatch(PreferenceManagementService pmService, String itemId) throws Exception {
        String itemType = null;
        String[] preferences = TCUtils.getTCPreferences(pmService, D9_ITEM_CODETYPE);
        if (preferences != null && preferences.length > 0) {
            List<String> list = new ArrayList<>(Arrays.asList(preferences));
            Optional<String> findAny = list.parallelStream().filter(str -> {
                if (str.startsWith(itemId.substring(0, 7))) {
                    return true;
                }
                return false;
            }).findAny();

            if (findAny.isPresent()) {
                itemType = findAny.get().split(":")[1];
            }
        }
        return itemType;
    }

    /**
     * 获取模型数量
     *
     * @param dmService 工具类
     * @param itemRev   对象版本
     * @return
     * @throws NotLoadedException
     */
    public static int getModelNum(DataManagementService dmService, ItemRevision itemRev) throws NotLoadedException {
        int count = 0;
        ModelObject[] modelObjects = TCUtils.getPropModelObjectArray(dmService, itemRev, TCItemConstant.REL_IMAN_SPECIFICATION);
        for (ModelObject obj : modelObjects) {
            if (!(obj instanceof Dataset)) {
                continue;
            }
            Dataset dataset = (Dataset) obj;
            String objectType = dataset.getTypeObject().getName();
            if (objectType.equalsIgnoreCase(TCDatasetEnum.PROASM.type()) || objectType.equalsIgnoreCase(TCDatasetEnum.PROPRT.type())) {
                count++;
            }
        }
        return count;
    }


    /**
     * 对象版本类型匹配
     *
     * @param objectType 对象版本类型
     * @return
     */
    private static boolean itemRevTypeAnyMatch(String objectType) {
        return ITEMREVTYPELIST.stream().anyMatch(str -> str.equals(objectType));
    }
}
