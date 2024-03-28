package com.foxconn.plm.tcservice.dcnreport.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.TCItemConstant;
import com.foxconn.plm.entity.constants.TCWorkflowStatusEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.feign.service.TcIntegrateClient;

import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.dcnreport.constant.BU;
import com.foxconn.plm.tcservice.dcnreport.constant.DCNReportConstant;
import com.foxconn.plm.tcservice.dcnreport.constant.DCNTypeEnum;
import com.foxconn.plm.tcservice.dcnreport.domain.*;
import com.foxconn.plm.tcservice.dcnreport.service.DCNReportService;
import com.foxconn.plm.tcservice.mapper.master.DCNReportMapper;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.foxconn.plm.tcservice.dcnreport.constant.DCNReportConstant.*;

/**
 * @Author HuashengYu
 * @Date 2022/10/21 14:24
 * @Version 1.0
 */
@Service
@Scope("request")
public class DCNReportServiceImpl implements DCNReportService {
    private static Log log = LogFactory.get();
    @Resource
    private DCNReportMapper dcnReportMapper;

    @Resource
    private TcIntegrateClient tcIntegrate;


    private String DCNExportDate = null;

    public static Properties prop = null;

    private Sheet DCNCostImpactCustomerSheet = null;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            prop = readPropertiesFile(CONFIGPATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Properties readPropertiesFile(String filePath) throws FileNotFoundException, IOException {
        InputStream inputStream = null;
        Properties props = null;
        try {
            ClassPathResource classPathResource = new ClassPathResource(filePath);
            inputStream = classPathResource.getInputStream();
            props = new Properties();
            props.load(new InputStreamReader(inputStream, "UTF-8"));
            return props;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
            }
        }
    }


    @Override
    @Transactional
    public void saveDCNReportData(List<DCNReportBean> list) {
        list.parallelStream().forEach(bean -> {
            if (StringUtil.isNotEmpty(bean.getProjectId())) {
                String result = tcIntegrate.getSTIProjectInfo(bean.getProjectId().replace("p", "").replace("P", ""));
                if (StringUtil.isNotEmpty(result)) {
                    STIProject stiProject = JSON.parseObject(result, STIProject.class);
                    bean.setCustomerType(stiProject.getCustomerName());
                    bean.setProductLine(stiProject.getPlatformFoundProductLine());
                }
            }
        });
        dcnReportMapper.insertDCNData(list);
    }

    @Override
    public JSONObject getLovList() {
        JSONObject json = new JSONObject();
        List<LovEntity> lov = dcnReportMapper.getLov();
        TreeMap<String, Object> buMap = new TreeMap<>();
        TreeMap<String, Object> customerMap = new TreeMap<>();
        TreeMap<String, Object> productLineMap = new TreeMap<>();
        TreeMap<String, Object> projectInfoMap = new TreeMap<>();
        if (CollectUtil.isNotEmpty(lov)) {
            for (LovEntity value : lov) {
                if (!"N/A".equals(value.getBu())) {
                    buMap.put(value.getBu().trim(), null);
                }
                if (StringUtil.isNotEmpty(value.getCustomer())) {
                    customerMap.put(value.getCustomer().trim(), null);
                }
                if (StringUtil.isNotEmpty(value.getProductLine())) {
                    productLineMap.put(value.getProductLine().trim(), null);
                }
                if (StringUtil.isNotEmpty(value.getProjectInfo())) {
                    projectInfoMap.put(value.getProjectInfo().trim(), null);
                }
            }
            json.put("buList", buMap.keySet());
            json.put("customerList", customerMap.keySet());
            json.put("productLineList", productLineMap.keySet());
            json.put("projectInfoList", projectInfoMap.keySet());
        }
        log.info("==>> json: " + JSONUtil.toJsonPrettyStr(json));
        return json;
    }

    /**
     * 获取可以联动的下拉框
     *
     * @return
     */
    @Override
    public List<LinkLovEntity> getLinkageLovList() {
        List<LovEntity> lov = dcnReportMapper.getLov();
        if (CollectUtil.isEmpty(lov)) {
            return new ArrayList<>();
        }
        lov.removeIf(bean -> "N/A".equals(bean.getBu()));
        return groupByLov(lov);
    }

    private Map<String, Map<String, Map<String, List<String>>>> groupbyLov(List<LovEntity> list) {
        Map<String, Map<String, Map<String, List<String>>>> retMap = new HashMap<>();
        Map<String, List<LovEntity>> buGroup = list.stream().collect(Collectors.groupingBy(bean -> bean.getBu()));
        buGroup.forEach((k1, v1) -> {
            Map<String, Map<String, List<String>>> customerMap = new HashMap<>();
            Map<String, List<LovEntity>> customerGroup = v1.stream().collect(Collectors.groupingBy(bean -> bean.getCustomer()));
            customerGroup.forEach((k2, v2) -> {
                Map<String, List<String>> productLineMap = new HashMap<>();
                Map<String, List<LovEntity>> productLineGroup = v2.stream().collect(Collectors.groupingBy(bean -> bean.getProductLine()));
                productLineGroup.forEach((k3, v3) -> {
                    Map<String, String> projectInfoMap = new HashMap<>();
                    List<String> projectInfoList = new ArrayList<>();
                    Map<String, List<LovEntity>> projectInfoGroup = v3.stream().collect(Collectors.groupingBy(bean -> bean.getProjectInfo()));
                    projectInfoGroup.forEach((k4, v4) -> {
                        projectInfoList.add(k4);
                    });
                    productLineMap.put(k3, projectInfoList);
                });
                customerMap.put(k2, productLineMap);
            });
            retMap.put(k1, customerMap);
        });
        return retMap;
    }

    private List<LinkLovEntity> groupByLov(List<LovEntity> list) {
        List<LinkLovEntity> resultList = new ArrayList<>();
        Map<String, List<LovEntity>> buGroup = list.stream().collect(Collectors.groupingBy(bean -> bean.getBu()));
        buGroup.forEach((k1, v1) -> {
            LinkLovEntity rootBean = new LinkLovEntity();
            rootBean.setValue(k1);
            Map<String, List<LovEntity>> customerGroup = v1.stream().collect(Collectors.groupingBy(bean -> bean.getCustomer()));
            customerGroup.forEach((k2, v2) -> {
                LinkLovEntity customerBean = new LinkLovEntity();
                customerBean.setValue(k2);
                rootBean.addChild(customerBean);
                Map<String, List<LovEntity>> productLineGroup = v2.stream().collect(Collectors.groupingBy(bean -> bean.getProductLine()));
                productLineGroup.forEach((k3, v3) -> {
                    LinkLovEntity productLineBean = new LinkLovEntity();
                    productLineBean.setValue(k3);
                    customerBean.addChild(productLineBean);
                    Map<String, List<LovEntity>> projectInfoGroup = v3.stream().collect(Collectors.groupingBy(bean -> bean.getProjectInfo()));
                    projectInfoGroup.forEach((k4, v4) -> {
                        LinkLovEntity projectInfoBean = new LinkLovEntity();
                        projectInfoBean.setValue(k4);
                        productLineBean.addChild(projectInfoBean);
                    });
                });
            });
            resultList.add(rootBean);
        });
        return resultList;
    }

    public JSONObject getFeeLovList() {
        JSONObject json = new JSONObject();
        List<FeeLovEntity> feeLov = dcnReportMapper.getFeeLov();
        if (CollectUtil.isEmpty(feeLov)) {
            return json;
        }

        feeLov.removeIf(entity -> StringUtil.isEmpty(entity.getProjectId()) || StringUtil.isEmpty(entity.getProjectName()) || StringUtil.isEmpty(entity.getOwner())); // 移除专案名或专案ID不存在的记录
        feeLov = feeLov.stream().filter(CollectUtil.distinctByKey(FeeLovEntity::getOwner)).collect(Collectors.toList()); // 移除相同的实际用户
        feeLov = feeLov.stream().filter(CollectUtil.distinctByKey(entity -> entity.getProjectId() + entity.getProjectName())).collect(Collectors.toList()); // 移除相同的专案信息

        if (CollectUtil.isEmpty(feeLov)) {
            return json;
        }

        TreeMap<String, Object> ownerMap = new TreeMap<>();
        List<ProjectBean> list = new ArrayList<>();
        for (FeeLovEntity entity : feeLov) {
            ProjectBean bean = new ProjectBean(entity.getProjectId(), entity.getProjectName());
            list.add(bean);
            if (StringUtil.isNotEmpty(entity.getOwner())) {
                ownerMap.put(entity.getOwner().trim(), null);
            }
        }

        json.put("projectInfoList", list);
        json.put("ownerMap", ownerMap.keySet());
        return json;
    }


    public List<LinkFeeLovEntity> getFeeLinkageLovList() {
        List<FeeLovEntity> feeLov = dcnReportMapper.getFeeLov();
        if (CollectUtil.isEmpty(feeLov)) {
            return new ArrayList<>();
        }
        feeLov.removeIf(bean -> "N/A".equals(bean.getBu()));
        return groupByFeeLov(feeLov);
    }

    private List<LinkFeeLovEntity> groupByFeeLov(List<FeeLovEntity> list) {
        List<LinkFeeLovEntity> resultList = new ArrayList<>();
        Map<String, List<FeeLovEntity>> buGroup = list.stream().collect(Collectors.groupingBy(FeeLovEntity::getBu));
        buGroup.forEach((k1, v1) -> {
            LinkFeeLovEntity rootBean = new LinkFeeLovEntity();
            rootBean.setValue(k1);
            Map<String, List<FeeLovEntity>> ownerGroup = v1.stream().collect(Collectors.groupingBy(FeeLovEntity::getOwner));
            ownerGroup.forEach((k2, v2) -> {
                LinkFeeLovEntity ownerBean = new LinkFeeLovEntity();
                ownerBean.setValue(k2);
                rootBean.addChild(ownerBean);
                Map<String, List<FeeLovEntity>> projectInfoGroup = v2.stream().collect(Collectors.groupingBy(entity -> entity.getProjectId() + entity.getProjectName()));
                projectInfoGroup.forEach((k3, v3) -> {
                    LinkFeeLovEntity projectInfoBean = new LinkFeeLovEntity();
                    projectInfoBean.setValue(k3);
                    ownerBean.addChild(projectInfoBean);
                });
            });
            resultList.add(rootBean);
        });
        return resultList;
    }

    @Override
    public List<DCNReportBean> getDCNRecordList(QueryEntity queryEntity) throws Exception {
        TCSOAServiceFactory tcsoaServiceFactory = null;
        try {
            boolean flag = false;
            String objectType = null;
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
            log.info("==>> queryEntity: " + queryEntity.toString());
            String bu = queryEntity.getBu();
            log.info("==>> bu: " + bu);
            String startTime = queryEntity.getStartDate();
            log.info("==>> startTime: " + startTime);
            String endTime = queryEntity.getEndDate();
            log.info("==>> endTime: " + endTime);
            String localStartTime = queryEntity.getStartDate() + " 00:00:00";
            log.info("==>> localStartTime： " + localStartTime);
            String localEndTime = queryEntity.getEndDate() + " 24:00:00";
            log.info("==>> localEndTime： " + localEndTime);
            Date startDate = sdf.parse(localStartTime);
            Date endDate = sdf.parse(localEndTime);
            String dcnRelease = queryEntity.getDcnRelease();
            log.info("==>> dcnRelease: " + dcnRelease);
            String dcnCostImpact = queryEntity.getDcnCostImpact();
            System.out.println("==>> dcnCostImpact: " + dcnCostImpact);

            List<DCNReportBean> totalDcnRecord = new ArrayList<>();
            List<DCNReportBean> dcnRecord = dcnReportMapper.getDCNRecord(queryEntity);
            if (CollectUtil.isEmpty(dcnRecord)) {
                return new ArrayList<>();
            }
            dcnRecord.forEach(bean -> {
                bean.setReason(REASONMAP.get(bean.getReason()));
            });
            totalDcnRecord.addAll(dcnRecord);
            if (queryEntity.getBu().equals(BU.DT.name())) {
                objectType = DCNTypeEnum.DT_DCN_REV.actualType();
            } else if (queryEntity.getBu().equals(BU.MNT.name())) {
                objectType = DCNTypeEnum.MNT_DCN_REV.actualType();
            } else if (queryEntity.getBu().equals(BU.PRT.name())) {
                objectType = DCNTypeEnum.PRT_DCN_REV.actualType();
            }

            if (DCNALLCOSTIMPACTFLAG.equals(dcnCostImpact) || DCNNOTCOSTIMPACTFLAG.equals(dcnCostImpact)) { // DCN 费用填写选择是All或者否才去系统查询
                List<DCNCreateBean> dcnCreateRecord = dcnReportMapper.getDCNCreateRecord(objectType, startDate, endDate); // 系统查找创建的DCN记录
                if (CollectUtil.isNotEmpty(dcnCreateRecord)) {
                    List<DCNCreateBean> retDCNCreateList = filterDCNCreateRecord(dcnRecord, dcnCreateRecord);
                    if (CollectUtil.isNotEmpty(retDCNCreateList)) {
                        totalDcnRecord.addAll(getCreateDCNReportBean(tcsoaServiceFactory.getDataManagementService(), retDCNCreateList, queryEntity.getBu(), dcnRelease));
                    }
                }
            }
            totalDcnRecord.removeIf(Objects::isNull);
            return filterDCNRecord(totalDcnRecord);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ObjectUtil.isNotNull(tcsoaServiceFactory)) {
                tcsoaServiceFactory.logout();
            }
        }
        return new ArrayList<>();

    }

    @Override
    public List<DCNFeeBean> getDCNFeeList(String projectId, String owner) {
        return getTotalDCNFee(projectId, owner);
    }

    /**
     * 获取DCN费用占比
     *
     * @param projectId
     * @param owner
     * @return
     */
    @Override
    public List<DCNTotalBean> getDCNFeePerByProject(String projectId, String owner) {
        List<DCNTotalBean> list = new ArrayList<>();
        List<DCNFeeBean> totalDCNFee = getTotalDCNFee("P" + projectId, owner);
        if (CollectUtil.isEmpty(totalDCNFee)) {
            return list;
        }

        totalDCNFee.removeIf(bean -> StringUtil.isEmpty(bean.getTotal()));
        totalDCNFee.forEach(bean -> {
            DCNTotalBean totalBean = new DCNTotalBean();
            totalBean.setUserId(bean.getUserId());
            totalBean.setUserName(bean.getUserName());
            String dcnFeePer = bean.getDcnFeePer();
            if (StringUtil.isNotEmpty(dcnFeePer)) {
                totalBean.setDcnFeePer(Float.parseFloat(dcnFeePer));
            }
//            totalBean.setDcnFeePer(bean.getDcnFeePer());
            list.add(totalBean);
        });
        return list;
    }


    private List<DCNFeeBean> getTotalDCNFee(String projectId, String owner) {
        List<DCNFeeBean> totalList = new ArrayList<>();
        List<DCNFeeBean> dcnFeeRecord = new ArrayList<>();
        List<DCNFeeBean> dcnFeeList = dcnReportMapper.getDCNFeeRecord(projectId, owner); // 查询DCN费用记录
        if (CollectUtil.isEmpty(dcnFeeList)) {
            return totalList;
        }
        List<DCNFeeBean> newMoldFeeList = dcnReportMapper.getNewMoldFeeRecord(owner); // 查询新模费用记录
        if (CollectUtil.isNotEmpty(dcnFeeList) && CollectUtil.isNotEmpty(newMoldFeeList)) {
            filterNewMoldRecord(dcnFeeList, newMoldFeeList, projectId); // 过滤新模费用记录
        }

        if (CollectUtil.isNotEmpty(dcnFeeList)) {
            dcnFeeRecord.addAll(dcnFeeList);
        }

        if (CollectUtil.isNotEmpty(newMoldFeeList)) {
            dcnFeeRecord.addAll(newMoldFeeList);
        }

        if (CollectUtil.isEmpty(dcnFeeRecord)) {
            return totalList;
        }

        Map<String, List<DCNFeeBean>> map = groupByUserAndReason(dcnFeeRecord);
        if (CollectUtil.isEmpty(map)) {
            return totalList;
        }
        List<DCNFeeBean> list = setNewMoldFeeAndDCNFee(map);// 设置DCN费用和新模费用
        if (CollectUtil.isEmpty(list)) {
            return totalList;
        }

        totalList = addIndexAndTotal(list);// 设置索引和新增汇总

        if (CollectUtil.isEmpty(totalList)) {
            return totalList;
        }
        Collections.sort(totalList);
        return totalList;
    }


    /**
     * 过滤新模费用记录
     *
     * @param dcnFeeList
     * @param newMoldFeeList
     */
    private void filterNewMoldRecord(List<DCNFeeBean> dcnFeeList, List<DCNFeeBean> newMoldFeeList, String projectId) {
        dcnFeeList.removeIf(bean -> StringUtil.isEmpty(bean.getUserId()) || StringUtil.isEmpty(bean.getUserName())); // 移除用户ID或用户名为空的记录
        ListIterator listIterator = newMoldFeeList.listIterator();
        while (listIterator.hasNext()) {
            DCNFeeBean newMoldBean = (DCNFeeBean) listIterator.next();
            // 判断工号姓名，图号是否一致，一致则移除掉
            boolean anyMatch = dcnFeeList.stream().anyMatch(dcnFeeBean -> dcnFeeBean.getUserId().equals(newMoldBean.getUserId()) && dcnFeeBean.getUserName().equals(newMoldBean.getUserName())
                    && dcnFeeBean.getModelNo().equals(newMoldBean.getModelNo())); // 判断DCN编号，版本号，新模图号是否一致
            if (anyMatch) {
                listIterator.remove();
            } else if (!newMoldBean.getProjectId().equals(projectId)) { // 如果新模费用指派的专案和查询界面的projectId不一致
                listIterator.remove();
            }
        }
    }


    /**
     * 通过用户和原因进行分组
     *
     * @param dcnFeeRecord
     * @return
     */
    private Map<String, List<DCNFeeBean>> groupByUserAndReason(List<DCNFeeBean> dcnFeeRecord) {
        Collections.sort(dcnFeeRecord); // 进行排序
        Map<String, List<DCNFeeBean>> resultMap = new LinkedHashMap<>();
        dcnFeeRecord.forEach(bean -> {
            List<DCNFeeBean> dcnFeeBeanList = resultMap.get(bean.getUserId() + "/" + bean.getUserName() + "/" + bean.getReason());
            if (dcnFeeBeanList == null) {
                dcnFeeBeanList = new ArrayList<>();
                dcnFeeBeanList.add(bean);
                resultMap.put(bean.getUserId() + "/" + bean.getUserName() + "/" + bean.getReason(), dcnFeeBeanList);
            } else {
                dcnFeeBeanList.add(bean);
            }
        });
        return resultMap;
    }

    /**
     * 设置索引
     *
     * @param dcnFeeRecord
     */
    private List<DCNFeeBean> addIndexAndTotal(List<DCNFeeBean> dcnFeeRecord) {
        Map<String, List<DCNFeeBean>> collect = dcnFeeRecord.stream().collect(Collectors.groupingBy(bean -> bean.getUserId() + bean.getUserName()));
        List<DCNFeeBean> resultList = new ArrayList<>();
        int index = 1;
        for (Map.Entry<String, List<DCNFeeBean>> entry : collect.entrySet()) {
            List<DCNFeeBean> value = entry.getValue();
            for (DCNFeeBean bean : value) {
                bean.setIndex(String.valueOf(index));
            }
            resultList.addAll(value);
            resultList.add(addTotal(value));
            index++;
        }
        return resultList;
    }

    /**
     * 设置DCN费用和新模费用
     *
     * @param map
     * @return
     */
    private List<DCNFeeBean> setNewMoldFeeAndDCNFee(Map<String, List<DCNFeeBean>> map) {
        List<DCNFeeBean> resultList = new ArrayList<>();
        for (Map.Entry<String, List<DCNFeeBean>> entry : map.entrySet()) {
            String key = entry.getKey();
            List<DCNFeeBean> value = entry.getValue();
            if (!key.endsWith("-")) {
                for (DCNFeeBean bean : value) {
                    if (StringUtil.isNotEmpty(bean.getModelNo()) && StringUtil.isNotEmpty(bean.getModelNoVersion())) {
                        String dcnFee = bean.getDcnFee();
                        String newMoldFee = "0";
                        List<DCNFeeBean> newMoldFeeList = dcnReportMapper.getNewMoldFee(bean.getModelNo());
                        if (CollectUtil.isNotEmpty(newMoldFeeList)) {
                            for (DCNFeeBean newMoldBean : newMoldFeeList) {
                                if ("01".equals(newMoldBean.getModelNoVersion())) {
                                    newMoldFee = newMoldBean.getNewMoldFee();
                                    break;
                                } else if ("A".equals(newMoldBean.getModelNoVersion())) {
                                    newMoldFee = newMoldBean.getNewMoldFee();
                                    break;
                                }
                            }
                        }
                        bean.setNewMoldFee(newMoldFee);
                        String dcnFeePer = calculateDcnFeePer(dcnFee, newMoldFee); // 计算DCN费用占比
                        bean.setDcnFeePer(dcnFeePer);
                    }
                }
            }
            resultList.addAll(value);
        }
        return resultList;
    }


    /**
     * 添加汇总bean
     *
     * @param list
     * @return
     */
    private DCNFeeBean addTotal(List<DCNFeeBean> list) {
        DCNFeeBean total = new DCNFeeBean();
        float totalDCNFee = 0;
        float totalNewMoldFee = 0;
        for (DCNFeeBean bean : list) {
            totalDCNFee += Float.parseFloat(bean.getDcnFee());
            totalNewMoldFee += Float.parseFloat(bean.getNewMoldFee());
            total.setIndex(bean.getIndex());
            total.setUserId(bean.getUserId());
            total.setUserName(bean.getUserName());
        }

        String totalDCNFeeStr = String.valueOf(totalDCNFee);
        if (totalDCNFeeStr.endsWith(".0")) {
            totalDCNFeeStr = totalDCNFeeStr.replace(".0", "");
        }

        total.setDcnFee(totalDCNFeeStr);

        String totalNewMoldFeeStr = String.valueOf(totalNewMoldFee);
        if (totalNewMoldFeeStr.endsWith(".0")) {
            totalNewMoldFeeStr = totalNewMoldFeeStr.replace(".0", "");
        }
        total.setNewMoldFee(totalNewMoldFeeStr);
        String totalDCNFeePer = calculateDcnFeePer(total.getDcnFee(), total.getNewMoldFee());
        total.setDcnFeePer(totalDCNFeePer);
        total.setTotal("total");
        return total;
    }


    /**
     * 计算DCN费用占比
     *
     * @param dcnFee
     * @param newMoldFee
     * @return
     */
    private String calculateDcnFeePer(String dcnFee, String newMoldFee) {
        String dcnFeePer = "0.0";
        if (!"0".equals(newMoldFee)) {
            dcnFeePer = String.format("%.4f", Float.parseFloat(dcnFee) / Float.parseFloat(newMoldFee));
        }
        return dcnFeePer;
    }

    private List<DCNCreateBean> filterDCNCreateRecord(List<DCNReportBean> dcnRecord, List<DCNCreateBean> dcnCreateRecord) {
        List<DCNCreateBean> finishDCNCreateList = Collections.synchronizedList(new ArrayList<>());
        dcnCreateRecord.parallelStream().forEach(createBean -> {
            boolean anyMatch = dcnRecord.stream().anyMatch(dcnReportBean -> dcnReportBean.getDcnNo().equals(createBean.getItemId()));
            if (!anyMatch) {
                finishDCNCreateList.add(createBean);
            }
        });
        return finishDCNCreateList;
    }


    private List<DCNReportBean> getCreateDCNReportBean(DataManagementService dmService, List<DCNCreateBean> DCNCreateList, String bu, String dcnRelease) {
        if (CollectUtil.isEmpty(DCNCreateList)) {
            return null;
        }
        List<DCNReportBean> dataDCNReportList = Collections.synchronizedList(new ArrayList<>());
        for (DCNCreateBean createBean : DCNCreateList) {
            try {
                ItemRevision DCNItemRev = (ItemRevision) TCUtils.findObjectByUid(dmService, createBean.getItemRevUid());
                ModelObject[] problemItemObjs = TCUtils.getPropModelObjectArray(dmService, DCNItemRev, TCItemConstant.REL_PROBLEMITEM);
                ModelObject[] solutionItemObjs = TCUtils.getPropModelObjectArray(dmService, DCNItemRev, TCItemConstant.REL_SOLUTIONITEM);
                if (ObjectUtil.isEmpty(TCUtils.checkModelObjTypeExist(dmService, problemItemObjs, D9_MEDESIGNREVISION))
                        && ObjectUtil.isEmpty(TCUtils.checkModelObjTypeExist(dmService, solutionItemObjs, D9_MEDESIGNREVISION))) { // 判断DCN问题项和解决方案项是否都没有ME设计对象
                    continue;
                };

                DCNReportBean bean = new DCNReportBean();
                bean.setBu(bu);
                bean.setDcnNo(TCUtils.getPropStr(dmService, DCNItemRev, TCItemConstant.PROPERTY_ITEM_ID));
                if (DCNRELEASEFLAG.equals(dcnRelease)) { // DCN状态为发布
                    if (TCUtils.isReleased(dmService, DCNItemRev)) {
                        bean.setStatus(TCWorkflowStatusEnum.Released.name());
                        dataDCNReportList.add(bean);
                    }
                } else if (DCNNOTRELEASEFLAG.equals(dcnRelease)) { // DCN状态为未发布
                    if (!TCUtils.isReleased(dmService, DCNItemRev)) {
                        dataDCNReportList.add(bean);
                    }
                } else if (DCNALLRELEASEFLAG.equals(dcnRelease)) { // DCN状态为All
                    if (TCUtils.isReleased(dmService, DCNItemRev)) {
                        bean.setStatus(TCWorkflowStatusEnum.Released.name());
                    }
                    dataDCNReportList.add(bean);
                }
            } catch (NotLoadedException e) {
                e.printStackTrace();
                log.error(e.getLocalizedMessage());
            }
        }
        
//        DCNCreateList.forEach(createBean -> {
//            try {
//                ItemRevision DCNItemRev = (ItemRevision) TCUtils.findObjectByUid(dmService, createBean.getItemRevUid());
//
//                DCNReportBean bean = new DCNReportBean();
//                bean.setBu(bu);
//                bean.setDcnNo(TCUtils.getPropStr(dmService, DCNItemRev, TCItemConstant.PROPERTY_ITEM_ID));
//                if (DCNRELEASEFLAG.equals(dcnRelease)) { // DCN状态为发布
//                    if (TCUtils.isReleased(dmService, DCNItemRev)) {
//                        bean.setStatus(TCWorkflowStatusEnum.Released.name());
//                        dataDCNReportList.add(bean);
//                    }
//                } else if (DCNNOTRELEASEFLAG.equals(dcnRelease)) { // DCN状态为未发布
//                    if (!TCUtils.isReleased(dmService, DCNItemRev)) {
//                        dataDCNReportList.add(bean);
//                    }
//                } else if (DCNALLRELEASEFLAG.equals(dcnRelease)) { // DCN状态为All
//                    if (TCUtils.isReleased(dmService, DCNItemRev)) {
//                        bean.setStatus(TCWorkflowStatusEnum.Released.name());
//                    }
//                    dataDCNReportList.add(bean);
//                }
//            } catch (NotLoadedException e) {
//                e.printStackTrace();
//                log.error(e.getLocalizedMessage());
//            }
//
//        });
        return dataDCNReportList;
    }


    /**
     * 过滤一个DCN编号一个费用都没填写和没有填写完整的情形
     *
     * @param list
     * @return
     */
    public List<DCNReportBean> filterDCNRecord(List<DCNReportBean> list) {
        List<DCNReportBean> retList = new ArrayList<>();
        Map<String, List<DCNReportBean>> dcnGroup = list.stream().collect(Collectors.groupingBy(bean -> bean.getDcnNo()));
        dcnGroup.forEach((key, value) -> {
            if (checkCostImpact(value)) {
                value.removeIf(bean -> StringUtil.isEmpty(bean.getCostImpact()));
                retList.addAll(value);
            } else {
                DCNReportBean dcnReportBean = value.get(0);
                dcnReportBean.setReason(REASONFLAG);
                dcnReportBean.setCostImpact("0");
                retList.add(dcnReportBean);
            }
        });
        Collections.sort(retList);
        return retList;
    }

    /**
     * 核对一个DCN里面是否编号存在已经填写
     *
     * @param list
     * @return
     */
    public Boolean checkCostImpact(List<DCNReportBean> list) {
        Boolean flag = false;
        for (DCNReportBean bean : list) {
            if (StringUtil.isNotEmpty(bean.getCostImpact())) {
                flag = true;
                break;
            }
        }
        return flag;
    }


    @Override
    public ByteArrayOutputStream export(QueryEntity queryEntity) {
        log.info("==>> queryEntity: " + queryEntity.toString());
        BU buEnum = BU.valueOf(queryEntity.getBu());
        String startDate = queryEntity.getStartDate();
        log.info("==>> startDate: " + startDate);
        String endDate = queryEntity.getEndDate();
        log.info("==>> endDate: " + endDate);
        Workbook wb = null;
        ByteArrayOutputStream out = null;
        try {
            DCNExportDate = startDate + "~" + endDate;
            switch (buEnum) {
                case DT:
                    wb = ExcelUtil.getWorkbookNew(DTTEMPLATEPATH);
                    break;
                case MNT:
                    wb = ExcelUtil.getWorkbookNew(MNTTEMPLATEPATH);
                    break;
            }

            if (wb == null) {
                throw new Exception("==>> 没有找到符合当前BU: " + buEnum.name() + ", 导出模板");
            }
            wb.setForceFormulaRecalculation(true);
            CellStyle cellStyle = ExcelUtil.getCellStyle(wb);
            List<DCNReportBean> dcnReportBeanList = dcnReportMapper.getDCNRecord(queryEntity);
            if (CollectUtil.isEmpty(dcnReportBeanList)) {
                log.info("==>> 此搜索条件, " + queryEntity.getBu() + ", " + queryEntity.getCustomer() + ", " + queryEntity.getProductLine() + ", " + queryEntity.getProjectId() + ", "
                        + queryEntity.getStartDate() + ", " + queryEntity.getEndDate() + ", 不存在记录");
                return null;
            }
            Map<String, Map<String, Map<String, List<DCNReportBean>>>> retMap = groupDCNRecordData(dcnReportBeanList);// 分组数据
            if (CollectUtil.isEmpty(retMap)) {
                throw new Exception("Summary DCN数据分组失败");
            }
            exportSummary(wb, retMap);

            Map<String, Map<String, Map<String, Map<String, Map<String, List<DCNReportBean>>>>>> groupByProjectIdMap = groupDCNRecordDataByProjectId(dcnReportBeanList);
            if (CollectUtil.isEmpty(groupByProjectIdMap)) {
                throw new Exception("DCN费用统计 DCN数据分组数据失败");
            }
            exportDCNCostCount(wb, groupByProjectIdMap);

            List<DCNReportBean> dcnRecordByTypeList = dcnReportMapper.getDCNRecordByType(queryEntity, Arrays.asList(SHEET_METAL, PLASTIC));
            if (CollectUtil.isNotEmpty(dcnRecordByTypeList)) {
                dcnRecordByTypeList.removeIf(bean -> StringUtil.isEmpty(bean.getModelNoPrefix()));
                dcnRecordByTypeList = filterDCNRecord(dcnRecordByTypeList);
                Map<String, List<DCNReportBean>> collectMap = dcnRecordByTypeList.stream().collect(Collectors.groupingBy(bean -> bean.getModelNoPrefix()));
                exportDCNList(wb, cellStyle, collectMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (wb != null) {
                try {
                    out = new ByteArrayOutputStream();
                    wb.write(out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    wb.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return out;
    }

    private Map<String, Map<String, Map<String, List<DCNReportBean>>>> groupDCNRecordData(List<DCNReportBean> list) {
        Map<String, Map<String, Map<String, List<DCNReportBean>>>> retMap = new HashMap<>();
        list.forEach(bean -> {
            bean.setReason(REASONMAP.get(bean.getReason()));
        });
        Map<String, List<DCNReportBean>> buGroup = list.stream().collect(Collectors.groupingBy(bean -> bean.getBu()));
        buGroup.forEach((k1, v1) -> {
            Map<String, Map<String, List<DCNReportBean>>> customerMap = new HashMap<>();
            Map<String, List<DCNReportBean>> customerGroup = v1.stream().collect(Collectors.groupingBy(bean -> bean.getCustomerType()));
            customerGroup.forEach((k2, v2) -> {
                Map<String, List<DCNReportBean>> reasonGroup = v2.stream().collect(Collectors.groupingBy(bean -> bean.getReason()));
                customerMap.put(k2, reasonGroup);
            });
            retMap.put(k1, customerMap);
        });
        return retMap;
    }


    private void exportSummary(Workbook wb, Map<String, Map<String, Map<String, List<DCNReportBean>>>> map) throws IOException {
        Sheet summarySheet = wb.getSheet(SUMMARY_SHEET_NAME);
        ExcelUtil.setValueAtForString(summarySheet, 1, ExcelUtil.getColumIntByString("E"), "DCN統計(" + DCNExportDate + ")");
        map.forEach((buKey, buValue) -> {
            buValue.forEach((customerKey, customerValue) -> {
                customerValue.forEach((reasonKey, reasonValue) -> {
                    {
                        try {
                            setDCNCountAndCostImpactValue(summarySheet, buKey + "," + customerKey + "," + reasonKey + "," + DCNNUMBER, reasonValue);
                            setDCNCountAndCostImpactValue(summarySheet, buKey + "," + customerKey + "," + reasonKey + "," + DCNCOSTIMPACT, reasonValue);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                try {
                    setProjectCount(summarySheet, buKey + "," + customerKey + "," + POJECTCOUNT, customerValue);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * 设置专案数量
     *
     * @param sheet
     * @param key
     * @param map
     * @throws IOException
     */
    private void setProjectCount(Sheet sheet, String key, Map<String, List<DCNReportBean>> map) throws IOException {
        int rowIndex = -1;
        int colIndex = -1;
        System.out.println("==>> DCN Report key: " + key);
        String index = prop.getProperty(key);
        if (StringUtil.isEmpty(index)) {
            log.warn("==>> DCN Report key: " + key + ", 没有匹配到");
            return;
        }
        rowIndex = Integer.parseInt(Optional.ofNullable(index.split(",")[0]).orElse("0")) - 1;
        colIndex = Optional.ofNullable(ExcelUtil.getColumIntByString(index.split(",")[1])).orElse(0);
        List<DCNReportBean> list = new ArrayList<>();
        Collection<List<DCNReportBean>> values = map.values();
        Iterator<List<DCNReportBean>> it = values.iterator();
        while (it.hasNext()) {
            List<DCNReportBean> next = it.next();
            list.addAll(next);
        }
        list = list.stream().filter(CollectUtil.distinctByKey(bean -> bean.getProjectId() + bean.getProjectName())).collect(Collectors.toList());
        ExcelUtil.setValueAtForDouble(sheet, rowIndex, colIndex, list.size());
    }


    /**
     * 通过项目ID进行分组
     *
     * @param list
     */
    private Map<String, Map<String, Map<String, Map<String, Map<String, List<DCNReportBean>>>>>> groupDCNRecordDataByProjectId(List<DCNReportBean> list) {
        Map<String, Map<String, Map<String, Map<String, Map<String, List<DCNReportBean>>>>>> retMap = new HashMap<>();
        list.removeIf(bean -> StringUtil.isEmpty(bean.getProjectName())); // 移除项目为空的记录
        Map<String, List<DCNReportBean>> buGroup = list.stream().collect(Collectors.groupingBy(bean -> bean.getBu()));
        buGroup.forEach((k1, v1) -> {
            Map<String, Map<String, Map<String, Map<String, List<DCNReportBean>>>>> projectIdMap = new HashMap<>();
            Map<String, List<DCNReportBean>> projectGroup = v1.stream().collect(Collectors.groupingBy(bean -> bean.getProjectId() + "&&" + bean.getProjectName()));
            projectGroup.forEach((k2, v2) -> {
                Map<String, Map<String, Map<String, List<DCNReportBean>>>> customerMap = new HashMap<>();
                Map<String, List<DCNReportBean>> customerGroup = v2.stream().collect(Collectors.groupingBy(bean -> bean.getCustomerType()));
                customerGroup.forEach((k3, v3) -> {
                    Map<String, Map<String, List<DCNReportBean>>> modelNoPrefixMap = new HashMap<>();
                    Map<String, List<DCNReportBean>> modelNoPrefixGroup = v3.stream().collect(Collectors.groupingBy(bean -> bean.getModelNoPrefix()));
                    modelNoPrefixGroup.forEach((k4, v4) -> {
                        Map<String, List<DCNReportBean>> reasonGroup = v4.stream().collect(Collectors.groupingBy(bean -> bean.getReason()));
                        modelNoPrefixMap.put(k4, reasonGroup);
                    });
                    customerMap.put(k3, modelNoPrefixMap);
                });
                projectIdMap.put(k2, customerMap);
            });
            retMap.put(k1, projectIdMap);
        });
        return retMap;
    }

    /**
     * 输出DCN费用统计
     *
     * @param wb
     * @param
     * @param map
     */
    private void exportDCNCostCount(Workbook wb, Map<String, Map<String, Map<String, Map<String, Map<String, List<DCNReportBean>>>>>> map) throws Exception {
        Sheet DCNCostImpactSheet = wb.getSheet(DCNCOSTCOUNT_SHEET_NAME);
        if (DCNCostImpactSheet == null) {
            throw new Exception(DCNCOSTCOUNT_SHEET_NAME + ", sheet页获取失败");
        }
        map.forEach((buKey, buValue) -> {
            buValue.forEach((projectInfoKey, projectInfoValue) -> {
                projectInfoValue.forEach((customerKey, customerValue) -> {
                    customerValue.forEach((modelNoPrefixKey, modelNoPrefixKeyValue) -> {
                        modelNoPrefixKeyValue.forEach((reasonKey, reasonValue) -> {
                            try {
                                setDCNCountAndCostImpactValue(DCNCostImpactSheet, buKey + "," + modelNoPrefixKey + "," + reasonKey + "," + DCNNUMBER, reasonValue);
                                setDCNCountAndCostImpactValue(DCNCostImpactSheet, buKey + "," + modelNoPrefixKey + "," + reasonKey + "," + DCNCOSTIMPACT, reasonValue);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                    try {
                        ExcelUtil.setValueAtForString(DCNCostImpactSheet, DCNCOSTCOUNT_SHEET_HEADSTARTROW - 1, ExcelUtil.getColumIntByString(DCNCOSTCOUNT_SHEET_STARTCOL), DCNCOSTCOUNT_SHEET_HEADER_NAME + "-" + customerKey);
                        ExcelUtil.setValueAtForString(DCNCostImpactSheet, DCNCOSTCOUNT_SHEET_CONTENTSTARTROW - 1, ExcelUtil.getColumIntByString(DCNCOSTCOUNT_SHEET_STARTCOL), projectInfoKey.split("\\&&")[1]); // 设置专案信息
                        ExcelUtil.setValueAtForString(DCNCostImpactSheet, DCNCOSTCOUNT_SHEET_TABLEHEADERROW - 1, ExcelUtil.getColumIntByString(DCNCOSTCOUNT_SHEET_HEADERCOL), DCNExportDate);

                        DCNCostImpactCustomerSheet = wb.getSheet(DCNCOSTCOUNT_SHEET_NAME + "-" + customerKey);
                        if (DCNCostImpactCustomerSheet == null) {
                            DCNCostImpactCustomerSheet = wb.createSheet(DCNCOSTCOUNT_SHEET_NAME + "-" + customerKey);

                            ExcelUtil.copySheetData(wb, DCNCostImpactSheet, DCNCOSTCOUNT_SHEET_HEADSTARTROW - 1, DCNCOSTCOUNT_SHEET_ENDROW, DCNCostImpactCustomerSheet); // 复制行
                        } else {
                            ExcelUtil.copySheetData(wb, DCNCostImpactSheet, DCNCOSTCOUNT_SHEET_CONTENTSTARTROW - 1, DCNCOSTCOUNT_SHEET_ENDROW, DCNCostImpactCustomerSheet); // 复制行
                        }

                        ExcelUtil.setValueAtForString(DCNCostImpactSheet, DCNCOSTCOUNT_SHEET_TABLEHEADERROW - 1, ExcelUtil.getColumIntByString(DCNCOSTCOUNT_SHEET_HEADERCOL), DCNCOSTCOUNT_SHEET_DATE_TEMEPLATE); // 修改DCN费用统计模板的日期字段
                        ExcelUtil.setValueAtForString(DCNCostImpactSheet, DCNCOSTCOUNT_SHEET_CONTENTSTARTROW - 1, ExcelUtil.getColumIntByString(DCNCOSTCOUNT_SHEET_STARTCOL), ""); // 修改DCN费用统计模板的专案信息字段
                        if (buKey.equals(BU.DT.name())) {
                            removeEntryDCNCount(DCNCostImpactSheet, DTDCNCostCountMap);
                        } else if (buKey.equals(BU.MNT.name())) {
                            removeEntryDCNCount(DCNCostImpactSheet, MNTDCNCostCountMap);
                        }
                        wb.setActiveSheet(wb.getSheetIndex(SUMMARY_SHEET_NAME));
                        wb.setSheetHidden(wb.getSheetIndex(DCNCOSTCOUNT_SHEET_NAME), true); // 隐藏费用统计sheet页
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
        });
    }

    /**
     * 恢复DCN费用统计模板数据
     *
     * @param sheet
     * @param map
     */
    private void removeEntryDCNCount(Sheet sheet, Map<String, String> map) throws IOException {
        int rowIndex = -1;
        int colIndex = -1;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            rowIndex = Integer.parseInt(Optional.ofNullable(value.split(",")[0]).orElse("0")) - 1;
            colIndex = Optional.ofNullable(ExcelUtil.getColumIntByString(value.split(",")[1])).orElse(0);
            ExcelUtil.setValueAtForDouble(sheet, rowIndex, colIndex, 0);
        }
    }


    /**
     * 设置DCN数量和费用字段
     *
     * @param sheet
     * @param key
     * @param list
     * @return
     * @throws IOException
     */
    private void setDCNCountAndCostImpactValue(Sheet sheet, String key, List<DCNReportBean> list) throws IOException {
        int rowIndex = -1;
        int colIndex = -1;
        System.out.println("==>> DCN Report key: " + key);
        String index = prop.getProperty(key);
        if (StringUtil.isEmpty(index)) {
            log.info("==>> DCN Report key: " + key + ", 没有匹配到");
            return;
        } else {
            rowIndex = Integer.parseInt(Optional.ofNullable(index.split(",")[0]).orElse("0")) - 1;
            colIndex = Optional.ofNullable(ExcelUtil.getColumIntByString(index.split(",")[1])).orElse(0);
            if (key.endsWith(DCNNUMBER)) {
                ExcelUtil.setValueAtForDouble(sheet, rowIndex, colIndex, list.size());
            } else if (key.endsWith(DCNCOSTIMPACT)) {
                ExcelUtil.setValueAtForDouble(sheet, rowIndex, colIndex, getDCNCostImpactCount(list));
            }
        }
    }


    private double getDCNCostImpactCount(List<DCNReportBean> list) {
        double retVal = 0.00;
        for (DCNReportBean bean : list) {
            if (StringUtil.isNotEmpty(bean.getCostImpact())) {
                retVal += Double.parseDouble(bean.getCostImpact());
            }
        }
        return retVal;
    }

    private void exportDCNList(Workbook wb, CellStyle cellStyle, Map<String, List<DCNReportBean>> map) throws IllegalAccessException {
        for (Map.Entry<String, List<DCNReportBean>> entry : map.entrySet()) {
            List<DCNReportBean> value = entry.getValue();
            if (SHEET_METAL.equals(entry.getKey())) {
                int k = 0;
                for (DCNReportBean bean : value) {
                    bean.setItem(String.valueOf(++k));
                    bean.setReason(REASONMAP.get(bean.getReason()));
                }
                Sheet sheet_metal_sheet = wb.getSheet(SHEET_METAL_SHEET_NAME);
                ExcelUtil.setCellValue(value, SHEET_METAL_SHEET_STARTROW, SHEET_METAL_SHEET_COLLENGTH, sheet_metal_sheet, cellStyle);
            } else if (PLASTIC.equals(entry.getKey())) {
                int k = 0;
                for (DCNReportBean bean : value) {
                    bean.setItem(String.valueOf(++k));
                    bean.setReason(REASONMAP.get(bean.getReason()));
                }
                Sheet plastic_sheet = wb.getSheet(PLASTIC_SHEET_NAME);
                ExcelUtil.setCellValue(value, PLASTIC_SHEET_STARTROW, PLASTIC_SHEET_COLLENGTH, plastic_sheet, cellStyle);
            }
        }
    }
}
