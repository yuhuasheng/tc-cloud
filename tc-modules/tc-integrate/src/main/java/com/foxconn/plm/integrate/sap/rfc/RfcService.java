package com.foxconn.plm.integrate.sap.rfc;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.param.PartPNRp;
import com.foxconn.plm.integrate.agile.domain.HHPNPojo;
import com.foxconn.plm.integrate.sap.rfc.domain.rp.PNSupplierInfo;
import com.foxconn.plm.integrate.sap.rfc.mapper.SAPSupplierMapper;
import com.foxconn.plm.integrate.sap.utils.DestinationUtils;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoTable;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2006_03.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ImanQuery;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.poi.util.SystemOutLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class RfcService {
    private static Log log = LogFactory.get();

    @Resource
    private SAPSupplierMapper sapSupplierMapper;


    public List<PartPNRp> isExistInSAP(List<PartPNRp> parts) throws Exception {
        log.info("======== Begin isExistInSAP =======");
        List<PartPNRp> notExists = new ArrayList<>();
        for (PartPNRp p : parts) {
            JCoDestination destination = DestinationUtils.getJCoDestination(p.getPlant());
            JCoFunction function = destination.getRepository().getFunctionTemplate("ZRFC_GET_PROD_MASTER").getFunction();
            JCoParameterList importParameterList = function.getImportParameterList();
            importParameterList.setValue("PARTNO", p.getItemNumber());
            importParameterList.setValue("PLANT", p.getPlant());
            function.execute(destination);
            JCoTable output = function.getTableParameterList().getTable("PROD_MASTER");
            int cnt=0;
            for (int i = 0; i < output.getNumRows(); i++) {
                output.setRow(i);
                String rev = (String) output.getValue("REVLV");
                log.info("plant:" + p.getPlant() + ",partNumber:" + p.getItemNumber() +" rev:"+rev);
                if(rev!=null&&(!("".equalsIgnoreCase(rev.trim())))){
                    cnt++;
                }
            }
            log.info("plant:" + p.getPlant() + ",partNumber:" + p.getItemNumber() +" "+cnt);
            if (cnt == 0) {
                notExists.add(p);
            }
        }
        log.info("=======  End isExistInSAP =======");
        return notExists;
    }

    public List<PNSupplierInfo> getSAPSupplierByTCDB(List<PNSupplierInfo> sourceList, String plant) {
        return sapSupplierMapper.selectInPartPn(sourceList, plant);
    }

    public List<PNSupplierInfo> compareSupplierInfo(List<PNSupplierInfo> sourceList, List<PNSupplierInfo> sapResultList) {
        List<PNSupplierInfo> result = new ArrayList<>();
        Map<String, List<PNSupplierInfo>> sapMap = sapResultList.stream().collect(Collectors.groupingBy(PNSupplierInfo::getPartPn,
                Collectors.toList()));
//       sapResultList.stream().collect(Collectors.toMap(PNSupplierInfo::getPartPn, ListUtil::of,
//                (v1, v2) -> v1.addAll(v2)));
        for (PNSupplierInfo sourceBean : sourceList) {
            List<PNSupplierInfo> sapBeans = sapMap.get(sourceBean.getPartPn());
            if (sapBeans == null || sapBeans.size() == 0) continue;
            boolean flag = false;
            for (PNSupplierInfo sapBean : sapBeans) {
                if (sapBean != null && compareSAPInfo(sapBean, sourceBean)) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                result.add(sapBeans.get(0));
            }
        }
        return result;
    }

    public boolean compareSAPInfo(PNSupplierInfo sapBean, PNSupplierInfo sourceBean) {
        String sapDesc = sapBean.getDescription();
        if (StringUtils.hasLength(sapDesc) && sapDesc.length() > 40) {
            sapDesc = sapDesc.substring(0, 40);
            sapBean.setDescription(sapDesc);
        }
        return (sapBean.getMfg() + sapBean.getMfgPn() + sapBean.getMfgZh() + sapDesc + sapBean.getUnit()).equalsIgnoreCase(sourceBean.getMfg() + sourceBean.getMfgPn() + sourceBean.getMfgZh() + sourceBean.getDescription() + sourceBean.getUnit());
    }

    public List<PNSupplierInfo> getSupplerInfo(String plant, List<PNSupplierInfo> supplierInfoList) {
        List<PNSupplierInfo> sapList = new ArrayList<>();
        try {
            if (supplierInfoList != null && supplierInfoList.size() > 0) {
                JCoDestination destination = DestinationUtils.getJCoDestination(plant);
                JCoFunction function = destination.getRepository().getFunctionTemplate("ZRFC_DPBU_MM_SUPPLIER").getFunction();
                JCoParameterList importParameterList = function.getImportParameterList();
                JCoParameterList importParameterListTable = function.getTableParameterList();
                JCoTable inputTable = importParameterListTable.getTable("HHPN");
                importParameterList.setValue("PLANT", plant);
                inputTable.deleteAllRows();
                for (int i = 0; i < supplierInfoList.size(); i++) {
                    PNSupplierInfo supplierInfo = supplierInfoList.get(i);
                    inputTable.appendRow();
                    inputTable.setRow(i);
                    inputTable.setValue("SIGN", "I");
                    inputTable.setValue("OPTION", "EQ");
                    inputTable.setValue("LOW", supplierInfo.getPartPn());
                }
                function.execute(destination);
                JCoParameterList resultTableParameter = function.getTableParameterList();
                JCoTable output = resultTableParameter.getTable("IT_OUT");
                for (int i = 0; i < output.getNumRows(); i++) {
                    output.setRow(i);
                    String mfg = (String) output.getValue("MAKERNAME");
                    String mfgPn = (String) output.getValue("MAKERPN");
                    String partPn = (String) output.getValue("MATNR");
                    PNSupplierInfo sapSupplierInfo = new PNSupplierInfo();
                    sapSupplierInfo.setPartPn(partPn);
                    sapSupplierInfo.setMfgPn(mfgPn);
                    sapSupplierInfo.setMfgZh(mfg);
                    sapList.add(sapSupplierInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        return sapList;
    }


    public void fillMfgInfos(List<PNSupplierInfo> hHPNPojos, String plant) throws Exception {
        JCoDestination destination = DestinationUtils.getJCoDestination(plant);
        for (PNSupplierInfo hHPNPojo : hHPNPojos) {
            try {
                JCoFunction function = destination.getRepository().getFunctionTemplate("ZRFC_GET_PROD_MASTER").getFunction();
                JCoParameterList importParameterList = function.getImportParameterList();
                importParameterList.setValue("PARTNO", hHPNPojo.getPartPn());
                importParameterList.setValue("PLANT", plant);
                function.execute(destination);
                JCoTable output = function.getTableParameterList().getTable("PROD_MASTER");
                for (int i = 0; i < output.getNumRows(); i++) {
                    output.setRow(i);
                    String mfgId = (String) output.getValue("MFRNR");
                    String REVLV = (String) output.getValue("REVLV");// rev
                    String description = (String) output.getValue("MAKTX");
                    String baseUnit = (String) output.getValue("MEINS");
                    String materialType = (String) output.getValue("MTART");
                    String MaterialGroup = (String) output.getValue("MATKL");
                    String mfrPn = (String) output.getValue("MFRPN");
                    String procurementType = (String) output.getValue("BESKZ"); //采购类型
                    hHPNPojo.setRev(REVLV);
                    hHPNPojo.setDescription(description);
                    hHPNPojo.setUnit(baseUnit);
                    hHPNPojo.setMfg(mfgId);
                    hHPNPojo.setMfgPn(mfrPn);
                    hHPNPojo.setMaterialType(materialType);
                    hHPNPojo.setMaterialGroup(MaterialGroup);
                    hHPNPojo.setProcurementType(procurementType);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }


    }


    public void upMaterialInfos(JCoDestination destination, List<HHPNPojo> hHPNPojos, String plant) {
        for (HHPNPojo hHPNPojo : hHPNPojos) {
            try {
                if (hHPNPojo.getIsExistInTC() == 1) {
                    continue;
                }
                log.info("begin get info from SAP ====" + hHPNPojo.getItemId() + "  " + hHPNPojo.getPlant());
                JCoFunction function = destination.getRepository().getFunctionTemplate("ZRFC_GET_PROD_MASTER").getFunction();
                JCoParameterList importParameterList = function.getImportParameterList();
                importParameterList.setValue("PARTNO", hHPNPojo.getItemId());
                importParameterList.setValue("PLANT", plant);
                function.execute(destination);
                JCoTable output = function.getTableParameterList().getTable("PROD_MASTER");
                for (int i = 0; i < output.getNumRows(); i++) {
                    output.setRow(i);
                    String REVLV = (String) output.getValue("REVLV");
                    String MATNR = (String) output.getValue("MATNR");
                    String materialType = (String) output.getValue("MTART");
                    String MaterialGroup = (String) output.getValue("MATKL");
                    String description = (String) output.getValue("MAKTX");
                    String baseUnit = (String) output.getValue("MEINS");
                    String procurementType = (String) output.getValue("BESKZ");
                    String mfgPN = (String) output.getValue("MFRPN");
                    String mfgId = (String) output.getValue("MFRNR");
                    hHPNPojo.setUnit(baseUnit);
                    hHPNPojo.setMfg(mfgId);
                    hHPNPojo.setMfgPN(mfgPN);
                    hHPNPojo.setMaterialType(materialType);
                    hHPNPojo.setMaterialGroup(MaterialGroup);
                    hHPNPojo.setDescr(description);
                    hHPNPojo.setRev(REVLV);
                    hHPNPojo.setProcurementType(procurementType);
                }
                log.info("end get info from SAP ====" + hHPNPojo.getItemId() + "  " + hHPNPojo.getPlant());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }


    }
   // @PostConstruct
   @XxlJob("syncSAPSupplier")
    @Transactional(rollbackFor = Exception.class)
    public void batchSAPSupplierToTCDB() throws Exception {
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            XxlJobHelper.log("start syncSAPSupplier ");
            sapSupplierMapper.deleteAll();
            XxlJobHelper.log("clean  tc sap_supplier db table ");
            String plant = XxlJobHelper.getJobParam();
            XxlJobHelper.log("syncSAPSupplier  plant :: " + plant);
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            Map<String, Object> queryResults = executeQuery(tcSOAServiceFactory.getSavedQueryService(), "__D9_Find_PartRev_ByType", new String[]{
                    "object_type"}, new String[]{("EDAComPart Revision")});
            if (queryResults.get("succeeded") == null) {
                XxlJobHelper.log("TC 未查询到零件");
                throw new Exception(" 未查询到零件");
            }
            ModelObject[] mds = (ModelObject[]) queryResults.get("succeeded");
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            dataManagementService.getProperties(mds, new String[]{"item_id"});
            List<PNSupplierInfo> mfgList = new LinkedList<>();
            for (ModelObject item : mds) {
                String partPn = item.getPropertyObject("item_id").getStringValue();
                PNSupplierInfo supplierInfo = new PNSupplierInfo();
                supplierInfo.setPartPn(partPn);
                mfgList.add(supplierInfo);
            }
            XxlJobHelper.log(" query tc EDAComPart rev size : " + mfgList.size());
            int splitSize = 1000;
            int splitCount = mfgList.size() / splitSize;
            List<CompletableFuture<List<PNSupplierInfo>>> futrueList = new ArrayList<>();
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            for (int i = 0; i < splitCount; i++) {

                List<PNSupplierInfo> tempList = new ArrayList<>();
                if (mfgList.size() > splitSize) {
                    tempList.addAll(mfgList.subList(0, splitSize));
                    mfgList.removeAll(tempList);
                } else {
                    tempList.addAll(mfgList);
                }
                CompletableFuture<List<PNSupplierInfo>> completableFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        List<PNSupplierInfo> sapList = getSupplerInfo(plant, tempList);
                        fillMfgInfos(sapList, plant);
                        return sapList;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return new ArrayList<>();
                }, executorService);
                futrueList.add(completableFuture);
            }
            CompletableFuture.allOf(futrueList.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<List<PNSupplierInfo>> future : futrueList) {
                List<PNSupplierInfo> sapMfgList = future.get();
                sapMfgList.forEach(e -> e.setPlant(plant));
                if (sapMfgList.size() > 0)
                    sapSupplierMapper.batchInsert(sapMfgList);
            }
            System.out.println("sync sap supplier complete !!");
            XxlJobHelper.log(" sync sap supplier complete !!");
        } catch (Exception e) {
            System.out.println("sync sap supplier fail !!!");
            e.printStackTrace();
            XxlJobHelper.log(e);
            XxlJobHelper.handleFail(e.getLocalizedMessage());
            throw e;
        }

    }

    public void batchUpPartInfo() throws Exception {
        TCSOAServiceFactory tcSOAServiceFactory = null;
        JCoDestination destination = null;
        try {
            destination = DestinationUtils.getJCoDestination("CHMB");

            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            SessionService sessionService = tcSOAServiceFactory.getSessionService();
            sessionService.refreshPOMCachePerRequest(true);
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            Map<String, Object> queryResults = executeQuery(tcSOAServiceFactory.getSavedQueryService(), "__D9_Find_PartRev_SAPRev",
                    new String[]{"object_type"}, new String[]{("EDAComPart Revision;D9_VirtualPartRevision;D9_PCB_PartRevision;D9_PCA_PartRevision;" +
                            "D9_FinishedPartRevision")});
            if (queryResults.get("succeeded") == null) {
                throw new Exception(" 未查询到图号");
            }
            ModelObject[] mds = (ModelObject[]) queryResults.get("succeeded");
            String failes = "";
            for (ModelObject m : mds) {
                ItemRevision rev = (ItemRevision) m;
                String uid = rev.getUid();
                TCUtils.getProperty(dataManagementService, rev, "item_id");
                String itemId = rev.get_item_id();
                try {

                    JCoFunction function = destination.getRepository().getFunctionTemplate("ZRFC_GET_PROD_MASTER").getFunction();
                    JCoParameterList importParameterList = function.getImportParameterList();
                    importParameterList.setValue("PARTNO", itemId);
                    importParameterList.setValue("PLANT", "CHMB");
                    function.execute(destination);
                    JCoTable output = function.getTableParameterList().getTable("PROD_MASTER");
                    for (int i = 0; i < output.getNumRows(); i++) {
                        output.setRow(i);
                        String REVLV = (String) output.getValue("REVLV");
                        String MATNR = (String) output.getValue("MATNR");
                        System.out.println(MATNR + " " + REVLV);

                        if (REVLV != null && !"".equalsIgnoreCase(REVLV)) {
                            TCUtils.byPass(sessionService, true);
                            TCUtils.setProperties(dataManagementService, rev, "d9_SAPRev", REVLV);
                            TCUtils.refreshObject(dataManagementService, new ModelObject[]{rev});
                        }
                    }

                } catch (Exception e) {
                    failes += uid + " " + itemId;
                }
            }
            System.out.println(failes);


        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }

    }


    private Map<String, Object> executeQuery(SavedQueryService queryService, String searchName, String[] keys, String[] values) {
        Map<String, Object> queryResults = new HashMap<>();
        try {
            ImanQuery query = null;
            SavedQuery.GetSavedQueriesResponse savedQueries = queryService.getSavedQueries();
            for (int i = 0; i < savedQueries.queries.length; i++) {
                if (savedQueries.queries[i].name.equals(searchName)) {
                    query = savedQueries.queries[i].query;
                    break;
                }
            }

            if (query == null) {
                queryResults.put("failed", "系统中未找到【" + searchName + "】查询..");
                return queryResults;
            }

            Map<String, String> entriesMap = new HashMap<>();
            SavedQuery.DescribeSavedQueriesResponse describeSavedQueriesResponse = queryService.describeSavedQueries(new ImanQuery[]{query});
            for (SavedQuery.SavedQueryFieldObject field : describeSavedQueriesResponse.fieldLists[0].fields) {
                String attributeName = field.attributeName;
                String entryName = field.entryName;
                entriesMap.put(attributeName, entryName);
            }

            String[] entries = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                entries[i] = entriesMap.get(keys[i]);
            }

            com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[] savedQueryInput =
                    new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[1];
            savedQueryInput[0] = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput();
            savedQueryInput[0].query = query;
            savedQueryInput[0].entries = entries;
            savedQueryInput[0].values = values;
            savedQueryInput[0].maxNumToReturn = 99999;
            com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse savedQueryResult =
                    queryService.executeSavedQueries(savedQueryInput);
            com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryResults found = savedQueryResult.arrayOfResults[0];
            queryResults.put("succeeded", found.objects);
            return queryResults;
        } catch (Exception e) {
            queryResults.put("failed", e.getMessage());
            return queryResults;
        }
    }


}
