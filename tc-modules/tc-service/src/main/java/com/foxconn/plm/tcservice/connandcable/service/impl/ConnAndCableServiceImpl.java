package com.foxconn.plm.tcservice.connandcable.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.dp.plm.privately.PrivaFileUtis;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.connandcable.domain.*;
import com.foxconn.plm.tcservice.connandcable.service.ConnAndCableService;
import com.foxconn.plm.tcservice.mapper.master.ConnAndCableMapper;
import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.file.FileUtil;
import com.foxconn.plm.utils.net.HttpUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.ItemUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.github.xiaoymin.swaggerbootstrapui.util.CommonUtils;
import com.google.gson.Gson;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.LOVService;
import com.teamcenter.services.strong.core._2006_03.DataManagement;
import com.teamcenter.services.strong.core._2013_05.LOV;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2007_06.SavedQuery;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.foxconn.plm.tcservice.connandcable.constant.ConnCableConstant.*;

/**
 * @Author HuashengYu
 * @Date 2022/10/7 8:50
 * @Version 1.0
 */
@Service
public class ConnAndCableServiceImpl implements ConnAndCableService {
    private static Log log = LogFactory.get();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private ConnAndCableMapper connAndCableMapper;

    private static final String D9_PREFIX = "d9_";

    private static final String POC_STR = "IR_";

    @Override
    public Map<String, Object> checkExcel(Workbook wb, String fileName) throws Exception {
        Map<String, Object> retMap = new HashMap<>();
        Sheet sheet = wb.getSheet(TEMPLATESHEETNAME);
        if (sheet==null) {
            throw new RuntimeException("excel 里面没有对应的sheet: " + TEMPLATESHEETNAME);
        }
        List<String> msgList = new ArrayList<>();
        Sheet fromSheet = wb.getSheet(TEMPLATESHEETNAME);
        checkExcelHeader(fromSheet, fileName); // 校验Excel表头
        List<List<String>> dataList = readSheet(fromSheet, CONTENTSTARTROW, -1); // 解析Excel内容
        if (CollectUtil.isEmpty(dataList)) {
            throw new RuntimeException("excel 解析失败");
        }
        checkExcelContent(dataList, msgList, retMap); // 校验内容
        List<CableBean> cableBeanList = (List<CableBean>) retMap.get("data");
        if (CollectUtil.isNotEmpty(cableBeanList)) {
            checkGroupIdAndHHPN(cableBeanList, msgList); // 校验Group和HHPN是否已经存在数据库表
        }
        retMap.put("msg", msgList);
        return retMap;
    }

    /**
     * 校验Excel表头
     *
     * @param fromSheet
     * @param fileName
     */
    private void checkExcelHeader(Sheet fromSheet, String fileName) {
        List<List<String>> dataList = readSheet(fromSheet, HEADERSTARTROW, HEADERENDROW);
        boolean flag = true;
        if (CollectUtil.isNotEmpty(dataList)) {
            for (int i = 0; i < dataList.size(); i++) {
                List<String> data = dataList.get(i);
                data.removeIf(StringUtil::isEmpty);
                List<String> list = null;
                if (i == (HEADERSTARTROW - 1)) {
                    list = new ArrayList<>(Arrays.asList(excelHeaderMap.get(HEADERSTARTROW)));
                } else if (i == (HEADERENDROW - 1)) {
                    list = new ArrayList<>(Arrays.asList(excelHeaderMap.get(HEADERENDROW)));
                }
                if (list != null) {
                    if (!data.containsAll(list) && !list.containsAll(data)) {
                        flag = false;
                        break;
                    }
                }
            }
        }
        if (!flag) {
            throw new RuntimeException(fileName + "模板表頭存在問題，請核對模板後重新上傳，謝謝");
        }
    }

    /**
     * 校验Excel内容
     *
     * @param dataList
     * @param msgList
     */
    private void checkExcelContent(List<List<String>> dataList, List<String> msgList, Map<String, Object> retMap) {
        List<CableBean> cableBeanList = new ArrayList<>();
        boolean exceptionFlag = false;
        for (int index = 0; index < dataList.size(); index++) {
            try {
                List<String> data = dataList.get(index);
                cableBeanList.addAll(CableBean.newCableBean(data, index + CONTENTSTARTROW, msgList));
            } catch (Exception e) {
                e.printStackTrace();
                msgList.add(e.getMessage());
                exceptionFlag = true;
            }
        }
        if (exceptionFlag) {
            retMap.put("exception", exceptionFlag);
            return;
        }
        cableBeanList.removeIf(bean -> bean.getGroupId()==null); // 移除GroupId为空的记录
        if (CollectUtil.isEmpty(cableBeanList)) {
            return;
        }
        Collections.sort(cableBeanList);
        Map<String, List<CableBean>> map = groupByGroupAndType(cableBeanList);
        cableBeanList = filterGroupRecord(map, msgList); // 过滤分组记录
        if (CollectUtil.isNotEmpty(cableBeanList)) {
            cableBeanList.removeIf(bean -> bean==null || bean.getFilterFlag()); // 移除为null和过滤标识位true的记录
            Collections.sort(cableBeanList);
            retMap.put("data", cableBeanList);
        }
    }

    /**
     * 过滤分组记录
     *
     * @param map
     * @return
     */
    private List<CableBean> filterGroupRecord(Map<String, List<CableBean>> map, List<String> msgList) {
        List<CableBean> cableBeanList = new ArrayList<>();
        Map<String, List<CableBean>> connMap = new LinkedHashMap<>();
        Map<String, List<CableBean>> cableMap = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (key.endsWith(CONN)) {
                connMap.put(key, value);
            } else if (key.endsWith(CABLE)) {
                cableMap.put(key, value);
            }
        });

        if (CollectUtil.isEmpty(connMap)) {
            return null;
        }

        for (Map.Entry<String, List<CableBean>> entry : connMap.entrySet()) {
            String key = entry.getKey();
            String groupId = key.split("-")[0];
            List<CableBean> connList = entry.getValue();
            List<CableBean> cableList = cableMap.get(groupId + "-" + CABLE);
            if (checkListNotEmpty(connList)) { // 当前为Conn,记录不全部为null集合, 记录当前的GroupId
                cableBeanList.addAll(connList);
                if (checkListNotEmpty(cableList)) { // 集合不全部为null
                    cableBeanList.addAll(cableList);
                } else {
                    CableBean cableBean = new CableBean();
                    cableBean.setGroupId(Integer.valueOf(groupId));
                    cableBean.setType(CABLE);
                    cableBeanList.add(cableBean);
                }
            } else {
                if (checkListNotEmpty(cableList)) {
                    cableList.forEach(bean -> {
                        msgList.add("【WARN】跳過 NO 為: " + bean.getGroupId() + ", HHPN為: " + bean.getHHPN() + "");
                    });
                }
            }
        }
        return cableBeanList;
    }

    /**
     * 判断list集合是否为空
     *
     * @param list
     * @return
     */
    private boolean checkListNotEmpty(List<CableBean> list) {
        boolean flag = false;
        for (CableBean bean : list) {
            if (!bean.getFilterFlag()) { // 判断过滤标识是否为false
                flag = true;
                break;
            }
        }
        return flag;
    }

    /**
     * 利用(GroupId和类型)进行分组
     *
     * @param list
     * @return
     */
    private Map<String, List<CableBean>> groupByGroupAndType(List<CableBean> list) {
        Map<String, List<CableBean>> resultMap = new LinkedHashMap<>();
        list.forEach(bean -> {
            List<CableBean> cableBeanList = resultMap.get(bean.getGroupId() + "-" + bean.getType());
            if (CollectUtil.isEmpty(cableBeanList)) {
                cableBeanList = new ArrayList<>();
                cableBeanList.add(bean);
                resultMap.put(bean.getGroupId() + "-" + bean.getType(), cableBeanList);
            } else {
                cableBeanList.add(bean);
            }
        });
        return resultMap;
    }

    /**
     * 校验Group和HHPN是否已经存在数据库表
     *
     * @param list
     * @param msgList
     */
    private void checkGroupIdAndHHPN(List<CableBean> list, List<String> msgList) {
        String connHHPN = null;
        String cableHHPN = null;
        Integer groupId = null;
        String type = null;
        ListIterator listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            CableBean bean = (CableBean) listIterator.next();
            groupId = bean.getGroupId();
            type = bean.getType();
            if (type.equals(CONN)) {
                connHHPN = bean.getHHPN();
                if (connAndCableMapper.checkConnRepeat(groupId, connHHPN) > 0) {
                    msgList.add("【INFO】GroupId：" + groupId + ", HHPN: " + connHHPN + ", 已經存在數據庫表CONNECTOR_TABLE, 無需重複導入");
                    listIterator.remove();
                }
            } else if (type.equals(CABLE)) {
                cableHHPN = bean.getHHPN();
                if (connAndCableMapper.checkCableRepeat(groupId, cableHHPN) > 0) {
                    msgList.add("【INFO】GroupId：" + groupId + ", HHPN: " + cableHHPN + ", 已經存在數據庫表CABLE_TABLE，無需重複導入");
                    listIterator.remove();
                }
            }
        }
    }

    @Override
    @Transactional
    public void saveData(List<CableBean> list, List<String> msgList, String userId) throws Exception {

        log.info("【INFO】************ 開始執行導入connAndCable ************");
        msgList.add("【INFO】************ 開始執行導入connAndCable ************");
        TCSOAServiceFactory tCSOAServiceFactory = null;

        try {

            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            if (CollectUtil.isNotEmpty(list)) {
                Map<String, List<CableBean>> listMap = groupByGroupAndType(list);
                listMap.forEach((key, value) -> {
                    if (key.endsWith(CONN)) {
                        log.info("start insert CONNECTOR_TABLE -->>>  -->>> " + value.size());
                        msgList.add("【INFO】start insert CONNECTOR_TABLE -->>>  -->>> " + value.size());
                        connAndCableMapper.insertConnRecord(value);
                        log.info("end insert CONNECTOR_TABLE @@@@@@@@@@@@");
                        msgList.add("【INFO】************ end insert CONNECTOR_TABLE ************");
                    } else if (key.endsWith(CABLE)) {
                        log.info("start insert CABLE_TABLE  -->>>  -->>> " + value.size());
                        msgList.add("【INFO】start insert CABLE_TABLE  -->>>  -->>> " + value.size());
                        connAndCableMapper.insertCableRecord(value);
                        log.info("end insert CABLE_TABLE @@@@@@@@@@@@");
                        msgList.add("【INFO】************ end insert CABLE_TABLE ************");
                    }
                });
                concurExecute(list, msgList, userId, tCSOAServiceFactory); // 并发执行创建零组件

            }

            log.info("【INFO】************ 结束执行导入connAndCable ************");
            msgList.add("【INFO】************ 结束执行导入connAndCable ************");
        } finally {
            try {
                tCSOAServiceFactory.logout();
            } catch (Exception r) {
            }

        }
    }

    @Override
    public ResponseEntity<byte[]> downloadTemplate(String currentTime) {
        HttpHeaders headers = new HttpHeaders();
        try {
            Workbook wb = ExcelUtil.getWorkbookNew(TEMPLATEPATH);
            wb.setForceFormulaRecalculation(true);
            Sheet sheet = ExcelUtil.getSheet(wb, TEMPLATESHEETNAME);
            CableBean cableBean = new CableBean();
            Integer maxGroupId = connAndCableMapper.getMAXGroupId();
            cableBean.setGroupId(maxGroupId + 1);
            List<CableBean> list = new ArrayList<>();
            list.add(cableBean);
            ExcelUtil.setCellValue(list, CONTENTSTARTROW, ENDCOL, sheet, ExcelUtil.getCellStyle(wb));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.close(); // 关闭此对象，便于后续删除此文件
            out.flush();
            headers.setContentDispositionFormData("attachment", currentTime + "_" + TEMPLATENAME);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<byte[]>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<byte[]>(headers, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public ResponseEntity<byte[]> exportConCaData(TCSOAServiceFactory tCSOAServiceFactory, JSONObject jsonData) throws Exception {

//        JSONObject parseObject = JSONObject.parseObject(jsonData);
        JSONArray groupIds = jsonData.getJSONArray("groupIds");
        List<Object> cocaInfos = getCCInfoByPN(jsonData);

        List<ConnectorInfo> coInfos = new ArrayList<>();
        List<CableInfo> caInfos = new ArrayList<>();
        for (int i = 0; i < cocaInfos.size(); i++) {
            Object object = cocaInfos.get(i);
            if (object instanceof ConnectorInfo) {
                ConnectorInfo connectorInfo = (ConnectorInfo) object;
                Integer groupId = connectorInfo.getGroupId();
                if (!groupIds.isEmpty() && !groupIds.contains(groupId)) {
                    continue;
                }
                coInfos.add(connectorInfo);
            }
            if (object instanceof CableInfo) {
                CableInfo cableInfo = (CableInfo) object;
                caInfos.add(cableInfo);
            }
        }

        Map<Integer, List<ConnectorInfo>> coInfoMap = coInfos.stream().collect(Collectors.groupingBy(ConnectorInfo::getGroupId));
        Map<Integer, List<CableInfo>> caInfoMap = caInfos.stream().collect(Collectors.groupingBy(CableInfo::getGroupId));
        PreferenceManagementService pmService = tCSOAServiceFactory.getPreferenceManagementService();
        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        FileManagementUtility fmuService = tCSOAServiceFactory.getFileManagementUtility();
        String[] cocaTemplates = TCUtils.getTCPreferences(pmService, "__D9_Connector_Cable_Template");
        Dataset dataset = (Dataset) TCUtils.findObjectByUid(dmService, cocaTemplates[0]);
        String tmpdir = System.getProperty(Access.check("java.io.tmpdir"));
        File file = TCUtils.downloadDataset(dmService, fmuService, dataset, tmpdir);
        String fileName = file.getName().replace("Template", String.valueOf(System.currentTimeMillis()));
        Workbook wb = writeExcel(file,coInfoMap,caInfoMap);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        out.flush();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<byte[]>(out.toByteArray(), headers, HttpStatus.OK);
    }

    @Override
    public List<Object> getCCInfoByPN(JSONObject jsonData) throws Exception {
//        JSONObject parseObject = JSONObject.parseObject(jsonData);
        JSONArray jsonArray = jsonData.getJSONArray("hhpn");
        String type = jsonData.getString("type");
        String hhpn = "";
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.size(); i++) {
                if (i != 0) {
                    hhpn += ";";
                }
                hhpn += jsonArray.getString(i);
            }
        }
        List<Object> connAndCableList = new ArrayList<>();
        List<String> groupIds = null;
        if ("".equals(hhpn)) {
            List<ConnectorInfo> connList = connAndCableMapper.getConnData(Access.check(groupIds));
            List<CableInfo> cableList = connAndCableMapper.getCableData(Access.check(groupIds));
            connAndCableList.addAll(connList);
            connAndCableList.addAll(cableList);
        } else {
            List<String> hhpns = Arrays.asList(hhpn.split(";"));
            if ("Connector".equals(type)) {
                groupIds = connAndCableMapper.queryConnGroupIdByPN(Access.check(hhpns));
            } else if ("Cable".equals(type)) {
                groupIds = connAndCableMapper.queryCableGroupIdByPN(Access.check(hhpns));
            }
            assert groupIds != null:"groupIds is NULL!";
            if (groupIds.size() != 0) {
                List<ConnectorInfo> connList = connAndCableMapper.getConnData(Access.check(groupIds));
                List<CableInfo> cableList = connAndCableMapper.getCableData(Access.check(groupIds));
                connAndCableList.addAll(connList);
                connAndCableList.addAll(cableList);
            } else {
                throw new Exception("未查询到数据!");
            }
        }
        return connAndCableList;
    }

    @Override
    public CoCaInfo getCoCaInfo(TCSOAServiceFactory tCSOAServiceFactory, String hhpn, String type, String source) throws Exception {
        CoCaInfo coCaInfo = null;
        String designPN = "";
        String desc = "";
        String mfg = "";
        if ("Connector".equals(type)) {
            coCaInfo = new ConnectorInfo();
        } else if ("Cable".equals(type)) {
            coCaInfo = new CableInfo();
        }
        if ("TC".equals(source)) {
            DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
            SavedQuery.ExecuteSavedQueriesResponse resp = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(),
                    "__D9_Find_Parts", new String[]{"item_id"}, new String[]{hhpn});
            ServiceData sd = resp.serviceData;
            if (sd.sizeOfPartialErrors() == 0) {
                ModelObject[] objs = resp.arrayOfResults[0].objects;
                if (objs.length == 0) {
                    throw new Exception("TC中未查询到【" + hhpn + "】数据！");
                }
                Item item = (Item) objs[0];
                ItemRevision itemRev = TCUtils.getItemLatestRevision(dmService, item);
                TCUtils.getProperty(dmService, itemRev, "d9_ManufacturerID");
                mfg = itemRev.getPropertyObject("d9_ManufacturerID").getStringValue();
                TCUtils.getProperty(dmService, itemRev, "d9_EnglishDescription");
                desc = itemRev.getPropertyObject("d9_EnglishDescription").getStringValue();
                ModelObject[] related = TCUtils.getPropModelObjectArray(dmService, itemRev, "TC_Is_Represented_By");
                if (related.length > 0) {
                    designPN = related[0].getPropertyObject("item_id").getStringValue();
                }
            } else {
                throw new Exception("查询错误，请联系系统管理员：" + sd.getPartialError(0));
            }
        } else if ("PNMS".equals(source)) {
            String[] url = TCUtils.getTCPreferences(tCSOAServiceFactory.getPreferenceManagementService(), "D9_SpringCloud_URL");
            String pnmsResult = HttpUtil.sendGet(url[0] + "/tc-integrate/pnms/getHHPNInfo", "hhpn=" + hhpn);
            if (pnmsResult.length() == 0) {
                throw new Exception("PNMS中未查询到【" + hhpn + "】数据！");
            }
            Gson gson = new Gson();
            Map<String, String> resultMap = gson.fromJson(pnmsResult, Map.class);
            if (resultMap.get("bupn") != null) {
                designPN = resultMap.get("bupn");
            }
            desc = resultMap.get("des");
            mfg = resultMap.get("mfg");
        }
        assert coCaInfo != null:"caCaInfo is NULL!";
        coCaInfo.setHhPN(hhpn);
        coCaInfo.setDesignPN(designPN);
        coCaInfo.setDescription(desc);
        coCaInfo.setSupplier(mfg);
        return coCaInfo;
    }

    @Override
    public int addCoCaInfo(TCSOAServiceFactory tCSOAServiceFactory, String hhpn,
                           String designPN, String desc, String mfg, String groupId, String type, String creator) throws Exception {
        String itemType = "";
        if ("Connector".equals(type)) {
            itemType = "EDAComPart";
        }
        if ("Cable".equals(type)) {
            itemType = "D9_CommonPart";
        }
        SavedQueryService sqService = tCSOAServiceFactory.getSavedQueryService();
        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        SavedQuery.ExecuteSavedQueriesResponse resp = TCUtils.execute2Query(sqService,
                "Item_Name_or_ID", new String[]{"item_id"}, new String[]{hhpn});
        ServiceData sd = resp.serviceData;
        if (sd.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = resp.arrayOfResults[0].objects;
            if (objs.length == 0) {
                ItemRevision edaComPart = TCUtils.createItem(dmService,
                        hhpn, "", itemType, hhpn, null);
                TCUtils.setProperties(dmService, edaComPart, "d9_ManufacturerID", mfg);
                TCUtils.setProperties(dmService, edaComPart, "d9_EnglishDescription", desc);
            }
        } else {
            throw new Exception("查询错误，请联系系统管理员：" + sd.getPartialError(0));
        }

        Integer count = -1;
        if ("Connector".equals(type)) {
            count = connAndCableMapper.queryConnInfoCount(hhpn);
        }
        if ("Cable".equals(type)) {
            count = connAndCableMapper.queryCableInfoCount(hhpn);
        }
        if (count > 0) {
            throw new Exception("HHPN，已存在！");
        }

        Integer affectedRow = 0;
        if ("Connector".equals(type)) {
            affectedRow = connAndCableMapper.addConn(hhpn, desc, mfg, groupId, creator,
                    dateFormat.format(new Date()), dateFormat.format(new Date()));
        } else if ("Cable".equals(type)) {
            Integer id = connAndCableMapper.queryHHPNEmptyByGroupId(Access.check(groupId));
            if (!"".equals(id) && id != null) {
                affectedRow = connAndCableMapper.updateCable1(Access.check(id), Access.check(hhpn), Access.check(designPN), Access.check(desc), Access.check(mfg));
            }else {
                affectedRow = connAndCableMapper.addCable(hhpn, designPN, desc, mfg, groupId, creator,
                        dateFormat.format(new Date()), dateFormat.format(new Date()));
            }
        }
        return affectedRow;
    }

    @Override
    public void addConnectorInfo(String hhpn, String desc, String mfg, String creator) throws Exception {
        Integer count = connAndCableMapper.queryConnInfoCount(hhpn);
        if (count > 0) {
            throw new Exception("HHPN，已存在！");
        }
        int maxGroupId = connAndCableMapper.queryMaxGroupId(Access.check(UUID.randomUUID().toString()));
        String newGroupId = String.valueOf(maxGroupId + 1);
        connAndCableMapper.addConnInfo(Access.check(hhpn), Access.check(desc), Access.check(mfg), Access.check(newGroupId), creator, dateFormat.format(new Date()), dateFormat.format(new Date()));
        connAndCableMapper.addCableInfo(Access.check(newGroupId), Access.check(creator), dateFormat.format(new Date()), dateFormat.format(new Date()));
    }

    @Override
    public void modifyConnector(TCSOAServiceFactory tCSOAServiceFactory, JSONObject jsonData) throws Exception {
        SavedQueryService sqService = tCSOAServiceFactory.getSavedQueryService();
        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        String itemId = jsonData.getString("hhpn");
        String desc = jsonData.getString("desc");
        String supplier = jsonData.getString("supplier");
        String cable = jsonData.getString("cable");
        SavedQuery.ExecuteSavedQueriesResponse resp = TCUtils.execute2Query(sqService,
                "零组件版本...", new String[]{"items_tag.item_id"}, new String[]{itemId});
        ModelObject[] objs = null;
        ServiceData sd = resp.serviceData;
        if (sd.sizeOfPartialErrors() == 0) {
            objs = resp.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("【TC】未查询到 " + itemId + " 数据！");
            }
        } else {
            throw new Exception("【TC】查询异常，请联系系统管理员！");
        }
        ItemRevision itemRev = (ItemRevision) objs[0];
        ServiceData sd1 = TCUtils.setProperties(dmService, itemRev, "d9_ManufacturerID", supplier);
        if (sd1.sizeOfPartialErrors() > 0) {
            throw new Exception("【TC】" + itemId + "设置 d9_ManufacturerID 属性失败！");
        }
        ServiceData sd2 = TCUtils.setProperties(dmService, itemRev, "d9_EnglishDescription", desc);
        if (sd2.sizeOfPartialErrors() > 0) {
            throw new Exception("【TC】" + itemId + "设置 d9_EnglishDescription 属性失败！");
        }
        if (!"1".equals(cable)) {
            connAndCableMapper.updateConn(itemId, desc, supplier);
        } else {
            connAndCableMapper.updateCable2(itemId, desc, supplier);
        }
    }

    @Override
    public void delCoCaInfo(JSONArray jsonData) throws Exception {
//        JSONArray jsonArray = JSONObject.parseArray(jsonData);
        List<String> connIds = new ArrayList<>();
        List<String> cableIds = new ArrayList<>();
        for (int i = 0; i < jsonData.size(); i++) {
            JSONObject obj = jsonData.getJSONObject(i);
            String id = obj.getString("id");
            String type = obj.getString("type");
            if ("Connector".equals(type)) {
                connIds.add(id);
            }
            if ("Cable".equals(type)) {
                cableIds.add(id);
            }
        }
        if (connIds.size() > 0) {
            connAndCableMapper.delConnInfo(connIds);
        }
        if (cableIds.size() > 0) {
            connAndCableMapper.delCableInfo(cableIds);
        }
    }

    @Override
    public String getCableRequestId(TCSOAServiceFactory tCSOAServiceFactory, String itemType) throws Exception {
        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        DataManagement.GenerateItemIdsAndInitialRevisionIdsProperties[] properties = new DataManagement.GenerateItemIdsAndInitialRevisionIdsProperties[1];
        DataManagement.GenerateItemIdsAndInitialRevisionIdsProperties property = new DataManagement.GenerateItemIdsAndInitialRevisionIdsProperties();
        property.count = 1;
        property.itemType = itemType;
        property.item = null;
        properties[0] = property;
        DataManagement.GenerateItemIdsAndInitialRevisionIdsResponse response = dmService.generateItemIdsAndInitialRevisionIds(properties);
        ServiceData serviceData = response.serviceData;
        int sizeOfPartialErrors = serviceData.sizeOfPartialErrors();
        if (sizeOfPartialErrors > 0) {
            String errorMessage = "";
            for (int i = 0; i < sizeOfPartialErrors; i++) {
                errorMessage = errorMessage + serviceData.getPartialError(i).toString();
            }
            throw new Exception(errorMessage);
        }
        String newItemId = "";
        Map<BigInteger, DataManagement.ItemIdsAndInitialRevisionIds[]> allNewIds = response.outputItemIdsAndInitialRevisionIds;
        for (Map.Entry<BigInteger, DataManagement.ItemIdsAndInitialRevisionIds[]> entry : allNewIds.entrySet()) {
            DataManagement.ItemIdsAndInitialRevisionIds[] outputs = entry.getValue();
            for (DataManagement.ItemIdsAndInitialRevisionIds result : outputs) {
                newItemId = result.newItemId;
            }
        }
        return newItemId;
    }

    @Override
    public List<String> getActualUser(TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        LOVService lovService = tCSOAServiceFactory.getLovService();
        LOV.InitialLovData initLovData = new LOV.InitialLovData();
        LOV.LovFilterData LovFilterData = new LOV.LovFilterData();
        LovFilterData.filterString = "";
        LovFilterData.maxResults = 3000;
        LovFilterData.numberToReturn = 3000;
        LovFilterData.order = 0;
        initLovData.filterData = LovFilterData;
        LOV.LOVInput lovInput = new LOV.LOVInput();
        lovInput.boName = "D9_CABDesignRevision";
        lovInput.operationName = "Create";
        Map<String, String[]> propertyValuesMap = new HashMap<>();
        propertyValuesMap.put("fnd0LOVContextObject", new String[]{"z0rJgx05ppJG1D"});
        propertyValuesMap.put("fnd0LOVContextPropName", new String[]{"contents"});
        lovInput.propertyValues = propertyValuesMap;
        initLovData.lovInput = lovInput;
        initLovData.propertyName = "d9_ActualUserID";
        LOV.LOVSearchResults results = lovService.getInitialLOVValues(initLovData);
        ServiceData sd = results.serviceData;
        if (sd.sizeOfPartialErrors() > 0) {
            throw new Exception(sd.getPartialError(0).getMessages()[0]);
        }
        List<String> userInfos = new ArrayList<>();
        LOV.LOVValueRow[] lovValues = results.lovValues;
        for (int i = 0; i < lovValues.length; i++) {
            String userInfo = lovValues[i].propDisplayValues.get("d9_UserInfo")[0];
            userInfos.add(userInfo);
        }
        return userInfos;
    }

    @Override
    public List<String> getCableTypeValues(TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        LOVService lovService = tCSOAServiceFactory.getLovService();
        LOV.InitialLovData initLovData = new LOV.InitialLovData();
        LOV.LovFilterData LovFilterData = new LOV.LovFilterData();
        LovFilterData.filterString = "";
        LovFilterData.maxResults = 2000;
        LovFilterData.numberToReturn = 50;
        LovFilterData.order = 0;
        initLovData.filterData = LovFilterData;
        LOV.LOVInput lovInput = new LOV.LOVInput();
        lovInput.boName = "D9_CableRequest";
        lovInput.operationName = "Create";
        Map<String, String[]> propertyValuesMap = new HashMap<>();
        propertyValuesMap.put("fnd0LOVContextObject", new String[]{""});
        propertyValuesMap.put("fnd0LOVContextPropName", new String[]{"contents"});
        lovInput.propertyValues = propertyValuesMap;
        initLovData.lovInput = lovInput;
        initLovData.propertyName = "object_name";
        LOV.LOVSearchResults results = lovService.getInitialLOVValues(initLovData);
        ServiceData sd = results.serviceData;
        if (sd.sizeOfPartialErrors() > 0) {
            throw new Exception(sd.getPartialError(0).getMessages()[0]);
        }
        List<String> cableTypes = new ArrayList<>();
        LOV.LOVValueRow[] lovValues = results.lovValues;
        for (int i = 0; i < lovValues.length; i++) {
            String cableType = lovValues[i].propDisplayValues.get("lov_values")[0];
            cableTypes.add(cableType);
        }
        return cableTypes;
    }

    @Override
    public void crateCableRequest(TCSOAServiceFactory tCSOAServiceFactory, String id, String type, String reason, String actualUser,
                                  String connector, String groupId, MultipartFile[] attachment, String cableList, String userId) throws Exception {
        List<TCCablePojo> tcCablePojos = JSONObject.parseArray(cableList, TCCablePojo.class);
        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        FileManagementUtility fmuService = tCSOAServiceFactory.getFileManagementUtility();
        SavedQueryService sqService = tCSOAServiceFactory.getSavedQueryService();
        SessionService sessionService = tCSOAServiceFactory.getSessionService();
        ItemRevision itemRev = TCUtils.createItem(dmService,
                id, "", "D9_CableRequest", type, null);
        Map<String, String> propMap = new HashMap<>();
        propMap.put("d9_ActualUserID", actualUser);
        propMap.put("d9_Connector", connector);
        propMap.put("d9_CCGroupID", groupId);
        propMap.put("d9_RequestReason", reason);
        propMap.put("object_name", type);
        TCUtils.setProperties(dmService, itemRev, propMap);


        String tmpdir = PrivaFileUtis.getTmpdir();
        Dataset dataset = null;
        if(attachment!=null){
            for (MultipartFile file : attachment) {
                String fileName = file.getOriginalFilename();
                String path = tmpdir + fileName;
                FileUtil.checkSecurePath(path);
                File targetFile = new File(path);
                file.transferTo(targetFile);
                String[] split = fileName.split("\\.");
                String extension = split[split.length - 1];
                List<String> typeList = getFileType(extension);
                String dsType = typeList.get(0);
                String refType = typeList.get(1);
                dataset = TCUtils.uploadDataset(dmService, fmuService, itemRev, path, refType,
                        fileName.substring(0, fileName.lastIndexOf(".")), dsType);
            }
        }
        User user = getUser(sqService, userId);
        TCUtils.getProperty(dmService, user, "home_folder");
        Folder homeFolder = user.get_home_folder();
        TCUtils.getProperty(dmService, itemRev, "items_tag");
        Item item = itemRev.get_items_tag();
        TCUtils.byPass(sessionService, true);
        TCUtils.addContents(dmService, homeFolder,item);
        TCUtils.byPass(sessionService, false);

        createCable(dmService, itemRev, tcCablePojos, actualUser,user);
        TCUtils.changeOwnShip(dmService,itemRev.get_items_tag(),user, (Group) user.get_default_group());
        TCUtils.changeOwnShip(dmService,itemRev,user, (Group) user.get_default_group());
        if(dataset!=null){
            TCUtils.changeOwnShip(dmService,dataset,user, (Group) user.get_default_group());
        }
    }

    @Override
    public String[] getNRValue(TCSOAServiceFactory tCSOAServiceFactory, String itemType, String propName) throws Exception {
        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        com.teamcenter.services.strong.core._2008_06.DataManagement.NRAttachInfo[] attachInfos = new com.teamcenter.services.strong.core._2008_06.DataManagement.NRAttachInfo[1];
        com.teamcenter.services.strong.core._2008_06.DataManagement.NRAttachInfo attachInfo = new com.teamcenter.services.strong.core._2008_06.DataManagement.NRAttachInfo();
        attachInfo.propName = propName;
        attachInfo.typeName = "D9_CABDesign";
        attachInfos[0] = attachInfo;
        com.teamcenter.services.strong.core._2008_06.DataManagement.GetNRPatternsWithCountersResponse nrPatternsWithCounters = dmService.getNRPatternsWithCounters(attachInfos);
        com.teamcenter.services.strong.core._2008_06.DataManagement.PatternsWithCounters[] patterns = nrPatternsWithCounters.patterns;
        return patterns[0].patternStrings;
    }

    @Override
    public String generateId(TCSOAServiceFactory tCSOAServiceFactory, String rule, String itemType) throws Exception {
        String id = "";
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[] ins = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[1];
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn in = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn();
        ins[0] = in;
        in.businessObjectName = itemType;
        in.clientId = "AutoAssignRAC";
        in.operationType = 1;
        Map<String, String> map = new HashMap<>();
        String prefix = "&quot;";
        String suffix = "-&quot;NNNNN";
        String mode = prefix + rule + suffix;
        map.put("item_id", mode);
        in.propertyNameWithSelectedPattern = map;
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesResponse response = tCSOAServiceFactory.getDataManagementService().generateNextValues(ins);
        com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput[] outputs = response.generatedValues;
        for (com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput result : outputs) {
            Map<String, com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue> resultMap = result.generatedValues;
            com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue generatedValue = resultMap.get("item_id");
            id = generatedValue.nextValue;
        }
        return id;
    }

    @Override
    public String generateVersion(TCSOAServiceFactory tCSOAServiceFactory, String rule, String itemRevType) throws Exception {
        String version = null;
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[] ins = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[1];
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn in = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn();
        ins[0] = in;
        in.businessObjectName = itemRevType;
        in.clientId = "AutoAssignRAC";
        in.operationType = 1;
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_revision_id", rule);
        in.propertyNameWithSelectedPattern = map;
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesResponse response = tCSOAServiceFactory.getDataManagementService().generateNextValues(ins);
        com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput[] outputs = response.generatedValues;
        for (com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput result : outputs) {
            Map<String, com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue> resultMap = result.generatedValues;
            com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue generatedValue = resultMap.get("item_revision_id");
            version = generatedValue.nextValue;
        }
        return version;
    }

    private void createCable(DataManagementService dmService, ItemRevision cableRequest,
                             List<TCCablePojo> tcCablePojos, String actualUser,User user) throws Exception {
        for (int i = 0; i < tcCablePojos.size(); i++) {
            TCCablePojo tcCablePojo = tcCablePojos.get(i);
            String itemId = tcCablePojo.getItemId();
            String itemRevId = tcCablePojo.getItemRev();
            String itemName = tcCablePojo.getItemName();
            String itemDesc = tcCablePojo.getItemDesc();
            String customer3dRev = tcCablePojo.getCustomer3DRev();
            String customer2dRev = tcCablePojo.getCustomer2DRev();
            String customerDrawingNumber = tcCablePojo.getCustomerDrawingNumber();
            String hhpn = tcCablePojo.getHhpn();
            String customer = tcCablePojo.getCustomer();
            String customerPN = tcCablePojo.getCustomerPN();
            String chineseDesc = tcCablePojo.getChineseDesc();
            String englishDesc = tcCablePojo.getEnglishDesc();

            ItemRevision itemRev = TCUtils.createItem(dmService,
                    itemId, itemRevId, "D9_CABDesign", itemName, null);
            TCUtils.getProperty(dmService,itemRev,"items_tag");
            TCUtils.changeOwnShip(dmService,itemRev,user, (Group) user.get_default_group());
            TCUtils.changeOwnShip(dmService,itemRev.get_items_tag(),user, (Group) user.get_default_group());
            Map<String, String> propMap = new HashMap<>();
            propMap.put("object_desc", itemDesc);
            propMap.put("d9_ActualUserID", actualUser);
            propMap.put("d9_Customer3DRev", customer3dRev);
            propMap.put("d9_Customer2DRev", customer2dRev);
            propMap.put("d9_CustomerDrawingNumber", customerDrawingNumber);
            propMap.put("d9_HHPN", hhpn);
            propMap.put("d9_Customer", customer);
            propMap.put("d9_CustomerPN", customerPN);
            propMap.put("d9_ChineseDescription", chineseDesc);
            propMap.put("d9_EnglishDescription", englishDesc);
            TCUtils.setProperties(dmService, itemRev, propMap);

            TCUtils.addRelation(dmService, cableRequest, itemRev, "D9_CableDesign_REL");
            TCUtils.getProperty(dmService, itemRev, "d9_CCGroupID");
            String groupId = itemRev.getPropertyObject("d9_CCGroupID").getStringValue();
            TCUtils.setProperties(dmService, itemRev, "d9_CCGroupID", groupId);
        }
    }


    private User getUser(SavedQueryService sqService, String userId) throws Exception {
        User user = null;
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(sqService, "__WEB_find_user",
                new String[]{"user_id"}, new String[]{userId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("未找到" + userId + "用户.");
            }
            user = (User) objs[0];
        } else {
            throw new Exception("查询" + userId + "用户失败：" + serviceData.getPartialError(0));
        }
        return user;
    }

    private List<String> getFileType(String extension) {
        String dsType, refType;
        switch (extension.toLowerCase(Locale.ENGLISH)) {
            case "pdf":
                dsType = "PDF";
                refType = "PDF_Reference";
                break;
            case "png":
                dsType = "Image";
                refType = "Image";
                break;
            case "7z":
                dsType = "D9_7Z";
                refType = "D9_7Z";
                break;
            case "zip":
                dsType = "Zip";
                refType = "ZIPFILE";
                break;
            case "bmp":
                dsType = "Bitmap";
                refType = "Image";
                break;
            case "stp":
                dsType = "D9_STEP";
                refType = "D9_STEP";
                break;
            case "dwg":
                dsType = "D9_AutoCAD";
                refType = "D9_AutoCAD";
            case "txt":
                dsType = "Text";
                refType = "Text";
                break;
            case "docx":
                dsType = "MSWordX";
                refType = "word";
                break;
            case "doc":
                dsType = "MSWord";
                refType = "word";
                break;
            case "pptx":
                dsType = "MSPowerPointX";
                refType = "powerpoint";
                break;
            case "ppt":
                dsType = "MSPowerPoint";
                refType = "powerpoint";
                break;
            case "xlsx":
                dsType = "MSExcelX";
                refType = "excel";
                break;
            case "xls":
                dsType = "MSExcel";
                refType = "excel";
                break;
            default:
                dsType = "JPEG";
                refType = "JPEG_Reference";
                break;
        }
        return CollUtil.newArrayList(dsType, refType);
    }


    /**
     * 并发执行创建零组件
     *
     * @param cableBeanList
     */
    private void concurExecute(List<CableBean> cableBeanList, List<String> msgList, String userId, TCSOAServiceFactory tcsoaServiceFactory) {

        cableBeanList.removeIf(bean -> StringUtil.isEmpty(bean.getHHPN()) || "null".equals(bean.getHHPN()));
        Map<CableBean, ItemRevision> resultMap = new HashMap<>();
        cableBeanList.parallelStream().forEach(bean -> {
            resultMap.put(bean, this.queryItem(bean, tcsoaServiceFactory));
        });

        List<CableBean> newItems = new ArrayList<>();
        resultMap.forEach((k, v) -> {
            if (v==null) {
                newItems.add(k);
            } else {
//                Map<String, String> propMap = getTCPropMap(k);
//                propMap.forEach((key, value) -> {
//                    if (key.startsWith(ATTR_PREFIX)) {
//                        TCUtils.setProperties(tcsoaServiceFactory.getDataManagementService(), v, key, value);
//                    }
//                });
                if (StringUtil.isNotEmpty(k.getHHPN())) {
                    log.info("【INFO】GroupId為: " + k.getGroupId() + ", HHPN為:" + k.getHHPN() + ", 已經存在於TC中");
                }
            }
        });

        if (CollectUtil.isEmpty(newItems)) {
            return;
        }
        List<List<CableBean>> splitList = CollectUtil.fixedGrouping(newItems, 15);
        TCUtils.byPass(tcsoaServiceFactory.getSessionService(), true); // 开启旁路
        splitList.forEach(newList -> {
            log.info("start connAndCable createItems  -->>>  -->>> " + newList.size());
            msgList.add("【INFO】start connAndCable createItems  -->>>  -->>> " + newList.size());
            log.info("running create items  -->>>");
            try {
                createItems(newList, msgList, userId, tcsoaServiceFactory);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getLocalizedMessage());
            }
            log.info("end create items  -->>>");
        });
        TCUtils.byPass(tcsoaServiceFactory.getSessionService(), false); // 关闭旁路
        log.info("end connAndCable createItems @@@@@@@@@@@@");
        msgList.add("【INFO】************ end connAndCable createItems ************");

    }

    public void createItems(List<CableBean> list, List<String> msgList, String userId, TCSOAServiceFactory tcsoaServiceFactory) throws Exception {
        log.info("running create items  -->>>");
//        msgList.add("running create items  -->>>");
        List<Map<String, String>> newMapList = list.stream().map(e -> {
            Map<String, String> propMap = getTCPropMap(e);
            return propMap;
        }).collect(Collectors.toList());

        ModelObject[] userObjects = TCUtils.executequery(tcsoaServiceFactory.getSavedQueryService(), tcsoaServiceFactory.getDataManagementService(), __WEB_FIND_USER_QUERY_NAME,
                __WEB_FIND_USER_QUERY_PARAMS, new String[]{userId});
        if (CollectUtil.isEmpty(userObjects)) {
            msgList.add("【ERROR】: " + userId + ", 用户在Teamcenter不存在");
            return;
        }

        User user = (User) userObjects[0];
        Group group = (Group) user.get_default_group();

        DataManagementService dmService = tcsoaServiceFactory.getDataManagementService();
        DataManagement.CreateItemsResponse response = ItemUtil.createItems(dmService, newMapList, null);
        DataManagement.CreateItemsOutput[] outputs = response.output;
        for (DataManagement.CreateItemsOutput output : outputs) {
            Item item = output.item;
            ItemRevision itemRev = output.itemRev;
            TCUtils.getProperty(dmService, itemRev, "item_id");
            try {
                String itemId = itemRev.get_item_id();
                Map<String, String> map = checkItemId(newMapList, itemId);
                if (CollectUtil.isNotEmpty(map)) {
                    map.forEach((k, v) -> {
                        int index = k.indexOf(D9_PREFIX);
                        if (index != -1) {
                            String propertyName = k.substring(0, index + D9_PREFIX.length()) + POC_STR
                                    + k.substring(index + D9_PREFIX.length());
                            ServiceData data = TCUtils.setProperties(dmService, itemRev, propertyName, v);
                            if (data.sizeOfPartialErrors() > 0) {
                                log.error("property name " + propertyName + ", is not exist");
                                TCUtils.setProperties(dmService, itemRev, k, v);
                            }
                        }
                    });
                }
                TCUtils.changeOwnShip(tcsoaServiceFactory.getDataManagementService(), item, user, group); // 更改对象的所有权
                TCUtils.changeOwnShip(tcsoaServiceFactory.getDataManagementService(), itemRev, user, group); // 更改对象版本的所有权
            } catch (NotLoadedException e) {
                e.printStackTrace();
                log.error(e.getLocalizedMessage());
            }
        }
        int errorSize = response.serviceData.sizeOfPartialErrors();
        if (errorSize > 0) {
            log.error("【ERROR】create item  response error size : " + errorSize);
            msgList.add("【ERROR】create item  response error size : " + errorSize);
            for (int i = 0; i < errorSize; i++) {
                log.error("【ERROR】create item response error info : " + Arrays.toString(response.serviceData.getPartialError(i).getMessages()));
                msgList.add("【ERROR】create item response error info : " + Arrays.toString(response.serviceData.getPartialError(i).getMessages()));
            }
        }

        log.info("ending create items  -->>>");
    }

    private Map<String, String> checkItemId(List<Map<String, String>> newMapList, String itemId) {
        Optional<Map<String, String>> findAny = newMapList.stream().filter(e -> {
            return e.get("item_id").equals(itemId);
        }).findAny();
        if (findAny.isPresent()) {
            return findAny.get();
        }
        return null;
    }

    private ItemRevision queryItem(CableBean bean, TCSOAServiceFactory tcsoaServiceFactory) {
        ItemRevision itemRevision = null;
        try {
            String itemID = bean.getHHPN();
            System.out.println("queryItem itemID ::: " + itemID);
            Item item = TCUtils.queryItemByIDOrName(tcsoaServiceFactory.getSavedQueryService(), tcsoaServiceFactory.getDataManagementService(), itemID, null);
            if (item != null) {
                itemRevision = TCUtils.getItemLatestRevision(tcsoaServiceFactory.getDataManagementService(), item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemRevision;
    }

    public Map<String, String> getTCPropMap(CableBean bean) {
        System.out.println("start -->>  getTCPropMap");
        Map<String, String> tcPropMap = new HashMap<>();
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            ReflectionUtils.makeAccessible(field);//使用 ReflectionUtils 替代
            TCPropName tcPropName = field.getAnnotation(TCPropName.class);
            if (tcPropName != null) {
                String tcProp = tcPropName.tcProperty();
                if (StringUtil.isNotEmpty(tcProp)) {
                    try {
                        Object o = field.get(bean);
                        if (o != null && !"null".equals(o)) {
                            tcPropMap.put(tcProp, (String) o);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("tcPropMap :: " + tcPropMap);
        return tcPropMap;
    }

    public List<List<String>> readSheet(Sheet sheet, int start, int end) {
        List<List<String>> list = new ArrayList<List<String>>();
        if (end > sheet.getLastRowNum() || end == -1) {
            end = sheet.getLastRowNum();
        }
        for (int i = start; i <= end; i++) {
            List<String> rowList = new ArrayList<String>();
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                if (cell != null) {
                    rowList.add(ExcelUtil.getCellValue(sheet, cell, "yyyy/MM/dd").trim());
                } else {
                    rowList.add("");
                }
            }
            list.add(rowList);
        }
        return list;
    }

    public Workbook writeExcel(File file, Map<Integer, List<ConnectorInfo>> connectorInfoMap, Map<Integer, List<CableInfo>> cableInfoInfoMap) {
        Workbook wb = null;
        Sheet sheet = null;
        FileOutputStream outputStream = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            if (file.getName().endsWith("xls")) {
                wb = new HSSFWorkbook(fis);
            } else {
                wb = new XSSFWorkbook(fis);
            }
            CellStyle cellStyle = wb.createCellStyle();
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            sheet = wb.getSheetAt(0);

            int startRow = 3;
            for (Map.Entry<Integer, List<ConnectorInfo>> item : connectorInfoMap.entrySet()) {
                Integer key = item.getKey();
                List<ConnectorInfo> connectorInfoList = item.getValue();
                List<CableInfo> cableInfoList = cableInfoInfoMap.get(key);

                for (int i = 0; i < connectorInfoList.size(); i++) {
                    ConnectorInfo connectorInfo = connectorInfoList.get(i);
                    String hhPN = connectorInfo.getHhPN();
                    String description = connectorInfo.getDescription();
                    String supplier = connectorInfo.getSupplier();
                    Row row = getRow(sheet, startRow + i);
                    if (i == 0) {
                        getCell(row, 0).setCellValue(key);
                    }
                    getCell(row, 1).setCellValue(hhPN);
                    getCell(row, 2).setCellValue(description);
                    getCell(row, 3).setCellValue(supplier);
                }

                if (cableInfoList == null) {
                    cableInfoList = new ArrayList<CableInfo>();
                }

                for (int i = 0; i < cableInfoList.size(); i++) {
                    CableInfo cableInfo = cableInfoList.get(i);
                    String hhPN = cableInfo.getHhPN();
                    String description = cableInfo.getDescription();
                    String supplier = cableInfo.getSupplier();
                    Row row = getRow(sheet, startRow + i);
                    getCell(row, 4).setCellValue(hhPN);
                    getCell(row, 5).setCellValue(description);
                    getCell(row, 6).setCellValue(supplier);
                }

                int stopRow = 0;
                if (connectorInfoList.size() > cableInfoList.size()) {
                    stopRow = connectorInfoList.size();
                } else if (connectorInfoList.size() < cableInfoList.size()) {
                    stopRow = cableInfoList.size();
                } else {
                    stopRow = connectorInfoList.size();
                }

                if (connectorInfoList.size() > 1 && cableInfoList.size() > 0) {
                    CellRangeAddress cellRangeAddress = new CellRangeAddress(startRow, (startRow + stopRow) - 1, 0, 0);
                    sheet.addMergedRegion(cellRangeAddress);
                }

                Row row = getRow(sheet, startRow);
                row.getCell(0).setCellStyle(cellStyle);

                startRow += stopRow;
            }

            outputStream = new FileOutputStream(file);
            wb.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try{
                if(fis!=null){
                    fis.close();
                }
            }catch (Exception e){
                e.printStackTrace();}


            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return wb;
    }
//
//    public Sheet getSheet(File file) {
//        Workbook wb = null;
//        Sheet sheet = null;
//        try {
//            if (file.getName().endsWith("xls")) {
//                wb = new HSSFWorkbook(new FileInputStream(file));
//            } else {
//                wb = new XSSFWorkbook(new FileInputStream(file));
//            }
//            sheet = wb.getSheetAt(0);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return sheet;
//    }

    public Row getRow(Sheet sheet, int rowNum) {
        Row row = sheet.getRow(rowNum);
        if (row == null) {
            row = sheet.createRow(rowNum);
        }
        return row;
    }

    public Cell getCell(Row row, int cellNum) {
        Cell cell = row.getCell(cellNum);
        if (cell == null) {
            cell = row.createCell(cellNum);
        }
        return cell;
    }
}
