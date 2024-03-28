package com.foxconn.plm.tcservice.ebom.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.*;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.ebom.domain.QuotationBOMBean;
import com.foxconn.plm.utils.tc.QueryUtil;
import com.foxconn.plm.utils.tc.StructureManagementUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.BOMWindow;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class SecondSourceServiceImpl {

    private final Log log = LogFactory.get();

    private static Pattern pattern = Pattern.compile("^.+?_P\\d+$"); // 以_P+数字结尾
    private ReentrantLock lock = new ReentrantLock(true); //设置公平锁

    public R<List<QuotationBOMBean>> sync2ndSourceQuotationBOM(List<QuotationBOMBean> list) {
        setIndex(list); // 设置索引
        List<QuotationBOMBean> totalList = new CopyOnWriteArrayList<>();
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        SavedQueryService savedQueryService = tcsoaServiceFactory.getSavedQueryService();
        StructureManagementService smService = tcsoaServiceFactory.getStructureManagementService();
        DataManagementService dmService = tcsoaServiceFactory.getDataManagementService();
        try {
            list.stream().parallel().forEach(e -> {
                System.out.println("==>> stdPN: " + e.getStdPn());
                totalList.add(e);
                try {
                    List<ItemRevision> searchList = getMatGroupItemRevBylock(savedQueryService, dmService, e.getStdPn());
                    if (CollUtil.isNotEmpty(searchList)) {
                        List<QuotationBOMBean> secondSourceGroupItemList = get2ndSourceGroupItemInfo(smService, dmService, searchList);
                        secondSourceGroupItemList.removeIf(bean -> bean.getStdPn().equals(e.getStdPn())); // 移除StdPn相同的记录
                        for (QuotationBOMBean bean : secondSourceGroupItemList) {
                            bean.setIndex(e.getIndex());
                            totalList.add(bean);
                        }
//                        totalList.addAll(secondSourceGroupItemList);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new RuntimeException(e1);
                }

            });

            if (CollUtil.isNotEmpty(totalList)) {
                Collections.sort(totalList);
            }
//            for (QuotationBOMBean bean : list) {
//                System.out.println("==>> stdPN: " + bean.getStdPn());
//                totalList.add(bean);
//                List<ItemRevision> searchList = getMatGroupItemRev(savedQueryService, dmService, bean.getStdPn());
//                if (CollUtil.isNotEmpty(searchList)) {
//                    List<QuotationBOMBean> secondSourceGroupItemList = get2ndSourceGroupItemInfo(smService, dmService, searchList);
//
//                    secondSourceGroupItemList.removeIf(e -> e.getStdPn().equals(bean.getStdPn())); // 移除StdPn相同的记录
//
//                    for (QuotationBOMBean info : secondSourceGroupItemList) {
//                        totalList.add(info);
//                    }
//                }
//            }
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        } finally {
            tcsoaServiceFactory.logout();
        }
        return R.success(totalList);
    }


    /**
     * 设置索引
     *
     * @param list
     */
    private void setIndex(List<QuotationBOMBean> list) {
        int index = 1;
        for (QuotationBOMBean bean : list) {
            bean.setIndex(index);
            index++;
        }

    }


    /**
     * 查询获取替代料群组集合
     *
     * @param queryService
     * @param dmService
     * @param item
     * @return
     * @throws Exception
     */
    private List<ItemRevision> getMatGroupItemRevBylock(SavedQueryService queryService, DataManagementService dmService, String item) throws Exception {
        List<ItemRevision> list = new CopyOnWriteArrayList<>();
        try {
//            lock.lock();
            log.info("find parts param: " + item);
            ModelObject[] results = QueryUtil.executeSOAQuery(queryService, TCSearchEnum.D9_FIND_MATERIALGROUP.queryName(), TCSearchEnum.D9_FIND_MATERIALGROUP.queryParams(), new String[]{item});
            if (ArrayUtil.isNotEmpty(results)) {
                Stream.of(results).forEach(e -> {
                    try {
                        if (!(e instanceof ItemRevision)) {
                            return;
                        }

                        ItemRevision itemRev = (ItemRevision) e;
                        String objectType = itemRev.getTypeObject().getName();
                        String itemId = TCUtils.getPropStr(dmService, itemRev, TCItemConstant.PROPERTY_ITEM_ID);
                        Matcher matcher = pattern.matcher(itemId.toUpperCase()); // 判断itemID是否以是"_P+数字结尾"
                        if (matcher.matches()) { // 判断替代料群组是否含有项目ID
                            return;
                        }

                        if ("D9_MaterialGroupRevision".equals(objectType)) {
                            dmService.refreshObjects(new ModelObject[]{itemRev});
                            String bu = TCUtils.getPropStr(dmService, itemRev, "d9_BU");
                            if (bu.toLowerCase().contains("mnt")) {
                                list.add(itemRev);
                            }
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        throw new RuntimeException(e1);
                    }
                });
            }
            return list;
        } finally {
//            lock.unlock();
        }
    }


    /**
     * 获取替代料群组物料信息
     *
     * @param smService
     * @param dmService
     * @param list
     * @return
     */
    private List<QuotationBOMBean> get2ndSourceGroupItemInfo(StructureManagementService smService, DataManagementService dmService, List<ItemRevision> list) {
        List<QuotationBOMBean> dataList = new CopyOnWriteArrayList<>();
        list.stream().parallel().forEach(itemRev -> {
            BOMWindow bomWindow = null;
            BOMLine topLine = null;
            try {
                List out = StructureManagementUtil.openBOMWindow(smService, itemRev);
                if (out != null && out.size() == 2) {
                    bomWindow = (BOMWindow) out.get(0);
                    topLine = (BOMLine) out.get(1);

                    dmService.refreshObjects(new ModelObject[]{topLine});
                    ModelObject[] children = TCUtils.getPropModelObjectArray(dmService, topLine, TCBOMLineConstant.PROPERTY_BL_ALL_CHILD_LINES);
                    if (ObjUtil.isNotEmpty(children)) {
                        for (ModelObject obj : children) {
                            BOMLine childBomLine = (BOMLine) obj;
                            String indentedTitle = TCUtils.getPropStr(dmService, childBomLine, "bl_indented_title");
                            log.info("==>> 替代料群组BOM标题: " + indentedTitle);

                            QuotationBOMBean bean = tcPropMapping(dmService, childBomLine, new QuotationBOMBean());
                            bean.setType("ALT");
                            bean.setSub(true);
                            bean.setCheck(true);
                            dataList.add(bean);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                if (ObjUtil.isNotEmpty(bomWindow)) {
                    StructureManagementUtil.closeBOMWindow(smService, bomWindow);
                }
            }
        });
//        for (ItemRevision itemRev : list) {
//            BOMWindow bomWindow = null;
//            BOMLine topLine = null;
//            try {
//                List out = StructureManagementUtil.openBOMWindow(smService, itemRev);
//                if (out != null && out.size() == 2) {
//                    bomWindow = (BOMWindow) out.get(0);
//                    topLine = (BOMLine) out.get(1);
//
//                    dmService.refreshObjects(new ModelObject[]{topLine});
//                    ModelObject[] children = TCUtils.getPropModelObjectArray(dmService, topLine, TCBOMLineConstant.PROPERTY_BL_ALL_CHILD_LINES);
//                    if (ObjUtil.isNotEmpty(children)) {
//                        for (ModelObject obj : children) {
//                            BOMLine childBomLine = (BOMLine) obj;
//                            String indentedTitle = TCUtils.getPropStr(dmService, childBomLine, "bl_indented_title");
//                            log.info("==>> 替代料群组BOM标题: " + indentedTitle);
//
//                            QuotationBOMBean bean = tcPropMapping(dmService, childBomLine, new QuotationBOMBean());
//                            bean.setType("ALT");
//                            dataList.add(bean);
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                if (ObjUtil.isNotEmpty(bomWindow)) {
//                    StructureManagementUtil.closeBOMWindow(smService, bomWindow);
//                }
//            }
//        }
        return dataList;
    }


    private static QuotationBOMBean tcPropMapping(DataManagementService dmService, BOMLine bomLine, QuotationBOMBean bean) throws NotLoadedException, IllegalAccessException {
        if (ObjUtil.isNotEmpty(bean) && ObjUtil.isNotEmpty(bomLine)) {
            ItemRevision itemRev = (ItemRevision) TCUtils.getPropModelObject(dmService, bomLine, TCBOMLineConstant.PROPERTY_BL_LINES_OBJECT);
            Field[] fields = bean.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                ReflectionUtils.makeAccessible(fields[i]);
                TCPropName tcPropName = fields[i].getAnnotation(TCPropName.class);
                if (ObjUtil.isNotEmpty(tcPropName)) {
                    String tcAttrName = tcPropName.tcProperty();
                    if (!tcAttrName.isEmpty()) {
                        Object value = "";
                        if (tcAttrName.startsWith("bl")) {
                            value = TCUtils.getPropStr(dmService, bomLine, tcAttrName);
                        } else {
                            value = TCUtils.getPropStr(dmService, itemRev, tcAttrName);
                        }

                        if (fields[i].getType() == Integer.class) {
                            if (value.equals("") || value == null) {
                                value = null;
                            } else {
                                value = Integer.parseInt((String) value);
                            }
                        }
                        fields[i].set(bean, value);
                    }
                }
            }
        }

        return bean;
    }
}
