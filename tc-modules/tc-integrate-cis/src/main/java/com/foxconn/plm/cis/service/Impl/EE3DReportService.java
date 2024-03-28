package com.foxconn.plm.cis.service.Impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.agile.api.APIException;
import com.agile.api.AgileSessionFactory;
import com.agile.api.IAgileSession;
import com.agile.api.IItem;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.cis.domain.*;
import com.foxconn.plm.cis.mapper.cisdell.CISDellMapper;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.cis.mapper.cis.CISMapper;
import com.foxconn.plm.cis.config.dataSource.DataSourceType;
import com.foxconn.plm.cis.config.dataSource.DynamicDataSourceContextHolder;
import com.foxconn.plm.entity.pojo.ReportPojo;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.tc.ItemUtil;
import com.foxconn.plm.utils.tc.QueryUtil;
import com.foxconn.plm.utils.tc.StructureManagementUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.FutureTask;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EE3DReportService {

    private final Log log = LogFactory.get();

    @Resource
    private TCService tcService;

    @Resource
    private CISMapper cisMapper;

    @Resource
    private CISDellMapper cisDellMapper;

    @Resource
    private TcMailClient tcMailClient;

    @Resource
    private TcIntegrateClient integrateClient;


    private IAgileSession agileSession;

    private final String[] PART_TYPES = new String[]{"Connector\\", "Optoelectronics\\LED", "Electromechanical Device\\Switch", "Mechanical " +
            "Hardware\\Heat Sink"};


    @Value("${agile.url:}")
    private String agileUrl;

    @Value("${agile.user:}")
    private String agileUser;

    @Value("${agile.pword:}")
    private String agilePw;

    @Value("${tc.ee-3d-report.dt.pcabom:}")
    private String agilePcaPns;

    @Value("${tc.ee-3d-report.dt.l6-ebom-folder-uid}")
    private String dtL6EBOMFolderUid;

    @Value("${tc.ee-3d-report.dt.projects.Dell}")
    private String dellProjects;

    @Value("${tc.ee-3d-report.dt.projects.HP}")
    private String hpProjects;

    @Value("${tc.ee-3d-report.dt.projects.Lenovo}")
    private String lenovoProjects;


    @Value("${tc.ee-3d-report.mnt.projects.Dell}")
    private String mntDellProjects;

    @Value("${tc.ee-3d-report.mnt.projects.HP}")
    private String mntHpProjects;

    @Value("${tc.ee-3d-report.mnt.projects.Lenovo}")
    private String mntLenovoProjects;

    @Value("${tc.ee-3d-report.mnt.projectIds}")
    private String mntProjectIds;

    private ForkJoinPool forkJoinPool;


    private List<EE3DUserPojo> ee3dUserList;

    private List<EE3DWFInfo> ee3DWFInfoList;

    @PostConstruct
    public void initExcelInfo() {
        try {
            Workbook workbook = ExcelUtil.getWorkbookNew("/templates/EE3DCISWFINFO.xlsx");
            if (ee3dUserList == null) {
                ee3dUserList = ExcelUtil.readSimpleExcel(workbook.getSheetAt(1), 1, EE3DUserPojo.class);
            }
            if (ee3DWFInfoList == null) {
                ee3DWFInfoList = ExcelUtil.readSimpleExcel(workbook.getSheetAt(0), 1, EE3DWFInfo.class);
            }
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<EE3DCISModelInfo> getNoCISModelInfos(Set<String> pnList, String bu, String customer) {
        String dataSource = "";
        if (pnList.size() > 0) {
            try {
                Set<String> mfgs = pnList.stream().map(e -> e.split("\\$")[0]).collect(Collectors.toSet());
                System.out.println(" query no cis list :: --- >> " + mfgs);
                String cisLibrary = getCisLibrary(bu, customer);
                dataSource = cisLibrary;
                List<EE3DCISModelInfo> noCisList = new ArrayList<>();
                if ("CIS Dell library".equalsIgnoreCase(cisLibrary)) {
                    DynamicDataSourceContextHolder.setDataSourceType(DataSourceType.CISDELL.name());
                    noCisList = cisDellMapper.getNoCISModelInfo(mfgs);
                } else {
                    DynamicDataSourceContextHolder.setDataSourceType(DataSourceType.CIS.name());
                    noCisList = cisMapper.getNoCISModelInfo(mfgs);
                }
                if (noCisList.size() != mfgs.size()) {
                    Set<String> cisMfgs = noCisList.stream().map(EE3DCISModelInfo::getMfgPn).collect(Collectors.toSet());
                    mfgs.removeAll(cisMfgs);
                    for (String noCisMfgPn : mfgs)
                        for (String mfgInfo : pnList) {
                            if (mfgInfo.contains(noCisMfgPn)) {
                                String[] pnInfos = mfgInfo.split("\\$");
                                EE3DCISModelInfo ee3DCISModelInfo = new EE3DCISModelInfo();
                                ee3DCISModelInfo.setHhPn(pnInfos[1]);
                                ee3DCISModelInfo.setMfgPn(pnInfos[0]);
                                ee3DCISModelInfo.setReMark("請 " + bu + " EE 啟動申请料号的流程");
                                ee3DCISModelInfo.setWfNode("/");
                                ee3DCISModelInfo.setUrl("");
                                ee3DCISModelInfo.setProductLine("");
                                ee3DCISModelInfo.setCisLibrary("");
                                ee3DCISModelInfo.setPartType("");
                                ee3DCISModelInfo.setProcessName("/");
                                ee3DCISModelInfo.setCisCustomer("/");
                                ee3DCISModelInfo.setDepartment("");
                                ee3DCISModelInfo.setProcessStatus("/");
                                ee3DCISModelInfo.setStandardPn("");
                                noCisList.add(ee3DCISModelInfo);
                            }
                        }
                }
                Map<String, EE3DWFInfo> wfMap = ee3DWFInfoList.stream().collect(Collectors.toMap(k -> k.getWfName() + k.getWorkItem(),
                        v -> v,
                        (v1, v2) -> v1));
                noCisList.forEach(e -> {
                    e.setCisLibrary(cisLibrary);
                    if ("CIS Dell library".equalsIgnoreCase(cisLibrary) && StringUtils.hasLength(e.getUrl())) {
                        e.setUrl(e.getUrl().replace("pdmcis", "PDMCISForDell"));
                    }
                    if (e.getProductLine() == null) {
                        e.setProductLine(bu);
                    }
                    setRemark(e, wfMap);
                });
                return noCisList;
            } catch (Exception e) {
                log.error(" getNoCISModelInfos error : " + dataSource + " -> " + pnList);
                e.printStackTrace();
            } finally {
                DynamicDataSourceContextHolder.clearDataSourceType();
            }
        }
        return new ArrayList<>();
    }

    /**
     * Remark生成邏輯：
     * 1. 如果Process Name is null, 則Remark = “請”+Product Line + “CE發起3D變更流程”
     * 2. 如果Process Name is not null，則Remark 依據欄位 Process Name & Process Status Mapping "CIS節點“Sheet頁的 ”工作流“ & ”節點名稱“，最後輸出 = “請”+Product Line +
     * Department（Product line與Department值不同時） +  "CIS節點“Sheet頁的”任務描述（中文）“
     */
    public void setRemark(EE3DCISModelInfo ee3DCISModelInfo, Map<String, EE3DWFInfo> wfMap) {
        if (StrUtil.isNotEmpty(ee3DCISModelInfo.getReMark())) {
            return;
        }
        String remark = "";
        String wfNode = "";
        if (StringUtils.hasLength(ee3DCISModelInfo.getProcessName()) && !"/".equalsIgnoreCase(ee3DCISModelInfo.getProcessName())) {
            EE3DWFInfo ee3DWFInfo = wfMap.get(ee3DCISModelInfo.getProcessName() + ee3DCISModelInfo.getProcessStatus());
            if (ee3DWFInfo != null) {
                String taskDesZh = ee3DWFInfo.getTaskDesZh();
                String temp = ee3DCISModelInfo.getProductLine();
                if (StringUtils.hasLength(ee3DCISModelInfo.getDepartment()) && !temp.equalsIgnoreCase(ee3DCISModelInfo.getDepartment())) {
                    temp += " " + ee3DCISModelInfo.getDepartment();
                }
                remark = "請 " + temp + " " + taskDesZh; //"請 " + temp + " " + ee3DCISModelInfo.getProcessStatus() + " " + taskDesZh;
                wfNode = ee3DCISModelInfo.getProductLine() + " " + ee3DWFInfo.getFunc();
            }
        } else {
            remark = "請 " + ee3DCISModelInfo.getProductLine() + " CE發起3D變更流程";
            wfNode = ee3DCISModelInfo.getProductLine() + " CE";
        }
        ee3DCISModelInfo.setWfNode(wfNode);
        ee3DCISModelInfo.setReMark(remark);
    }

    public void initForkJoinPool() {
        if (forkJoinPool == null) {
            int cpuCount = Runtime.getRuntime().availableProcessors();
            if (cpuCount < 8) {
                cpuCount = 8;
            }
            forkJoinPool = new ForkJoinPool(cpuCount * 2);
        }
    }


    public List<EE3DReportBean> getAllReport(List<EE3DProjectBean> data) {
        long startTime = System.currentTimeMillis();
        if (data == null) {
            data = new ArrayList<>();
            List<EE3DProjectBean> dtList = getDTProjectList();
            List<EE3DProjectBean> mntList = getMNTProjectList(mntProjectIds);
            data.addAll(mntList);
            data.addAll(dtList);
        }
        List<EE3DReportBean> list = new ArrayList<>();
        initForkJoinPool();
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        try {
            SessionService sessionService = SessionService.getService(tcsoaServiceFactory.getConnection());
            loadFolderProp(sessionService);
            ForkJoinTask<List<EE3DReportBean>> dtCallTask = getDTProjectInfoParallel(sessionService, tcsoaServiceFactory, data);
            ForkJoinTask<List<EE3DReportBean>> mntCallTask = getMntProjectInfoParallel(sessionService, tcsoaServiceFactory, data);
            list.addAll(dtCallTask.get());
            list.addAll(mntCallTask.get());
            list.sort(Comparator.comparing(e -> e.getBu() + e.getCustomer() + e.getProjectSeries() + e.getProjectName() + e.getPhase() + e.getVersion() + e.getCategory() + e.getPartType()));
            long endTime = System.currentTimeMillis();
            System.out.println("all cost time s :: " + (endTime - startTime) / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tcsoaServiceFactory.logout();
        }
        return list;
    }


    public List<EE3DReportBean> getAllReportNew() {
        long startTime = System.currentTimeMillis();
        List<EE3DProjectBean> data = getMNTProjectList(mntProjectIds);
        List<EE3DReportBean> list = new ArrayList<>();
        initForkJoinPool();
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        try {
            SessionService sessionService = SessionService.getService(tcsoaServiceFactory.getConnection());
            loadFolderProp(sessionService);
            ForkJoinTask<List<EE3DReportBean>> dtCallTask = getMntProjectInfoParallel(sessionService, tcsoaServiceFactory, data);
            list.addAll(dtCallTask.get());
            List<EE3DReportBean> dtData = getDTReportBeanListByPCA(sessionService, tcsoaServiceFactory, agilePcaPns);
            list.addAll(dtData);
            list.sort(Comparator.comparing(e -> e.getBu() + e.getCustomer() + e.getProjectSeries() + e.getProjectName() + e.getPhase() + e.getVersion() + e.getCategory() + e.getPartType()));
            long endTime = System.currentTimeMillis();
            System.out.println("all cost time s :: " + (endTime - startTime) / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tcsoaServiceFactory.logout();
        }
        return list;
    }


    public List<EE3DReportBean> getAllReport() {
        long startTime = System.currentTimeMillis();
        List<EE3DReportBean> list = new ArrayList<>();
        initForkJoinPool();
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        try {
            SessionService sessionService = SessionService.getService(tcsoaServiceFactory.getConnection());
            loadFolderProp(sessionService);
            List<ForkJoinTask<List<EE3DReportBean>>> callTaskList = getDT_EE3DReportBeanList(tcsoaServiceFactory);
            List<ForkJoinTask<List<EE3DReportBean>>> mntCallTaskList = getMNT_EE3DReportBeanList(tcsoaServiceFactory);
            callTaskList.addAll(mntCallTaskList);
            List<EE3DReportBean> allProjectData = new ArrayList<>();
            for (ForkJoinTask<List<EE3DReportBean>> task : callTaskList) {
                try {
                    if (task != null) {
                        allProjectData.addAll(task.get());
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            list = getReportListParallel(allProjectData, sessionService, tcsoaServiceFactory).get();
            list.sort(Comparator.comparing(e -> e.getBu() + e.getCustomer() + e.getProjectSeries() + e.getProjectName() + e.getPhase() + e.getVersion() + e.getCategory() + e.getPartType()));
            long endTime = System.currentTimeMillis();
            System.out.println("all cost time s :: " + (endTime - startTime) / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tcsoaServiceFactory.logout();
        }
        return list;
    }

    //  get project lov
    public List<EE3DProjectBean> getProjectInfoTree() {
        String[] fields = new String[]{"bu", "customer", "projectSeries", "projectName"};
        List<EE3DProjectBean> projectData = new ArrayList<>();
        List<EE3DProjectBean> dtList = getDTProjectList();
        List<EE3DProjectBean> mntList = getMNTProjectList(mntProjectIds);
        projectData.addAll(mntList);
        projectData.addAll(dtList);
        projectData = convertProjectTreeBean(projectData, fields);
        return projectData;
    }

    List<EE3DProjectBean> getMNTProjectList(String projectIds) {
        List<EE3DProjectBean> allProject = new ArrayList<>();
        String[] projectIdArray = projectIds.split(",");
        for (String projectId : projectIdArray) {
            try {
                List<ReportPojo> projects = integrateClient.queryProjectById(projectId);
                if (projects != null && projects.size() > 0) {
                    ReportPojo p = projects.get(0);
                    EE3DProjectBean projectBean = new EE3DProjectBean();
                    projectBean.setProjectName(p.getProjectName());
                    projectBean.setProjectSeries(p.getSeries());
                    projectBean.setId(p.getProjectId());
                    projectBean.setCustomer(p.getCustomer());
                    projectBean.setBu("MNT");
                    allProject.add(projectBean);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return allProject;
    }

    List<EE3DProjectBean> getDTProjectList() {
        List<EE3DProjectBean> dtList = new ArrayList<>();
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        try {
            DataManagementService ds = tcsoaServiceFactory.getDataManagementService();
            Folder ebomFolder = TCUtils.findFolderByUid(ds, dtL6EBOMFolderUid);
            List<EE3DReportBean> list = new ArrayList<>();
            list.addAll(getDTReportPojoList("Dell", dellProjects));
            list.addAll(getDTReportPojoList("Lenovo", lenovoProjects));
            list.addAll(getDTReportPojoList("HP", hpProjects));
            dtList = list.parallelStream().peek(e -> {
                Folder folder = null;
                String childUid = tcService.getChildFolderUidByName(dtL6EBOMFolderUid, e.getProjectSeries());
                try {
                    if (childUid != null)
                        folder = TCUtils.findFolderByUid(ds, childUid);
                } catch (ServiceException serviceException) {
                    serviceException.printStackTrace();
                }
                if (folder == null) {
                    System.out.println("Exception folder uid :: :" + childUid);
                }
                // Folder folder = folderMap.get(e.getProjectSeries());// findChildFolderByName(ebomFolder, , ds);
                e.setTempFolder(folder);
            }).filter(e -> e.getTempFolder() != null).map(e -> getReportBeansByFolder(e, "projectName", ds)).flatMap(Collection::stream).map(e -> {
                EE3DProjectBean projectBean = new EE3DProjectBean();
                projectBean.setBu("DT");
                projectBean.setCustomer(e.getCustomer());
                projectBean.setProjectSeries(e.getProjectSeries());
                projectBean.setProjectName(e.getProjectName());
                projectBean.setId(e.getTempFolder().getUid());
                return projectBean;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tcsoaServiceFactory.logout();
        }
        return dtList;
    }

    List<EE3DReportBean> getDTReportPojoList(String customer, String projects) {
        List<EE3DReportBean> list = new ArrayList<>();
        for (String series : projects.split(",")) {
            EE3DReportBean reportPojo = new EE3DReportBean();
            reportPojo.setCustomer(customer);
            reportPojo.setProjectSeries(series);
            list.add(reportPojo);
        }
        return list;
    }


    static List<EE3DProjectBean> convertProjectTreeBean(List<EE3DProjectBean> list, String[] fieldNames) {
        List<EE3DProjectBean> resultList = new ArrayList<>();
        if (fieldNames.length > 0) {
            String fieldName = fieldNames[0];
            Map<String, List<EE3DProjectBean>> map = list.stream().collect(Collectors.groupingBy(e -> {
                String v = "";
                try {
                    v = (String) Objects.requireNonNull(BeanUtils.getPropertyDescriptor(EE3DProjectBean.class, fieldName)).getReadMethod().invoke(e);
                } catch (Exception e1) {
                    System.out.println("exception projectId : " + e.getId() + " projectName : " + e.getProjectName());
                    e1.printStackTrace();
                }
                return v;
            }, Collectors.toList()));
            if (fieldNames.length > 1) {
                fieldNames = ArrayUtil.sub(fieldNames, 1, fieldNames.length);
            } else {
                fieldNames = new String[0];
            }
            for (Map.Entry<String, List<EE3DProjectBean>> entry : map.entrySet()) {
                EE3DProjectBean bean = new EE3DProjectBean();
                if (fieldNames.length == 0 && entry.getValue().size() > 0) {
                    bean = entry.getValue().get(0);
                }
                bean.setValue(entry.getKey());
                bean.setChilds(convertProjectTreeBean(entry.getValue(), fieldNames));
                resultList.add(bean);
            }

        }
        return resultList;
    }

    List<Map<String, Object>> convertProjectTree(List<ReportPojo> list, String[] fieldNames) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        if (fieldNames.length > 0) {
            String fieldName = fieldNames[0];
            Map<String, List<ReportPojo>> map = list.stream().collect(Collectors.groupingBy(e -> {
                String v = "";
                try {
                    v = (String) Objects.requireNonNull(BeanUtils.getPropertyDescriptor(ReportPojo.class, fieldName)).getReadMethod().invoke(e);
                } catch (Exception e1) {
                    System.out.println("exception projectId : " + e.getProjectId());
                    e1.printStackTrace();
                }
                return v;
            }, Collectors.toList()));
            if (fieldNames.length > 1) {
                fieldNames = ArrayUtil.sub(fieldNames, 1, fieldNames.length);
                for (Map.Entry<String, List<ReportPojo>> entry : map.entrySet()) {
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("value", entry.getKey());
                    resultMap.put("childs", convertProjectTree(entry.getValue(), fieldNames));
                    resultList.add(resultMap);
                }
            }
        }
        return resultList;
    }


    public List<EE3DReportBean> getReportListParallel(Stream<EE3DReportBean> stream, SessionService sessionService,
                                                      TCSOAServiceFactory tcsoaServiceFactory) {
        return stream.map(e -> fillBOMToItems(e, sessionService, tcsoaServiceFactory))
                .filter(Objects::nonNull)
                .collect(Collectors.toConcurrentMap(e -> e.getBu() + e.getCustomer() + e.getProjectSeries() + e.getProjectName() + e.getPhase() + e.getVersion(), e -> e, (v1, v2) -> {
                    if (v1.getItems() == null) {
                        v1.setItems(new HashSet<>());
                    }
                    if (v2.getItems() != null) {
                        v1.getItems().addAll(v2.getItems());
                    }
                    return v1;
                })).values().parallelStream().map(e -> convertBomToBean(e,
                        tcsoaServiceFactory)).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());

    }


    public ForkJoinTask<List<EE3DReportBean>> getReportListParallel(List<EE3DReportBean> projectData, SessionService sessionService,
                                                                    TCSOAServiceFactory tcsoaServiceFactory) {
        return forkJoinPool.submit(() -> getReportListParallel(projectData.parallelStream(), sessionService, tcsoaServiceFactory));

    }

    public EE3DReportBean fillBOMToItems(EE3DReportBean projectBean, SessionService sessionService, TCSOAServiceFactory tcsoaServiceFactory) {
        try {
            ItemRevision eebom = projectBean.getBom();
            getProperty(tcsoaServiceFactory.getDataManagementService(), eebom, "items_tag");
            Set<ItemRevision> itemRevs = getBOMItems(eebom, tcsoaServiceFactory.getStructureManagementService(), sessionService);
            projectBean.setItems(itemRevs);
            return projectBean;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error", e);
        }
        return null;
    }

    public List<EE3DReportBean> convertBomToBean(EE3DReportBean projectBean, TCSOAServiceFactory tcsoaServiceFactory) {
        try {

            List<EE3DReportBean> reportBeans = getItemsClsData(projectBean.getItems(), projectBean.getCustomer(), projectBean.getBu(),
                    tcsoaServiceFactory.getDataManagementService());
            for (EE3DReportBean itemBeans : reportBeans) {
                itemBeans.setProjectSeries(projectBean.getProjectSeries());
                itemBeans.setProjectName(projectBean.getProjectName());
                itemBeans.setBu(projectBean.getBu());
                itemBeans.setPhase(projectBean.getPhase());
                itemBeans.setVersion(projectBean.getVersion());
            }
            return reportBeans;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error", e);
        }
        return null;
    }

    public List<ForkJoinTask<List<EE3DReportBean>>> getMNT_EE3DReportBeanList(TCSOAServiceFactory tcsoaServiceFactory) {
        List<ForkJoinTask<List<EE3DReportBean>>> mntCallList = new ArrayList<>();
        DataManagementService ds = tcsoaServiceFactory.getDataManagementService();
        SavedQueryService queryService = tcsoaServiceFactory.getSavedQueryService();
        try {
            mntCallList.add(getMntProjectInfoParallel(queryService, ds, "Dell", mntDellProjects));
            mntCallList.add(getMntProjectInfoParallel(queryService, ds, "HP", mntHpProjects));
            mntCallList.add(getMntProjectInfoParallel(queryService, ds, "Lenovo", mntLenovoProjects));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error", e);
        }
        return mntCallList;
    }

    public FutureTask<List<EE3DReportBean>> putTaskThread(Callable<List<EE3DReportBean>> callable) {
        FutureTask<List<EE3DReportBean>> task = new FutureTask<>(callable);
        new Thread(task).start();
        return task;
    }

    Folder getFolderByUid(String uid, DataManagementService ds) {
        ServiceData sd = ds.loadObjects(new String[]{uid});
        if (sd != null && sd.sizeOfPlainObjects() > 0) {
            return (Folder) sd.getPlainObject(0);
        }
        return null;
    }

    public List<ForkJoinTask<List<EE3DReportBean>>> getDT_EE3DReportBeanList(TCSOAServiceFactory tcsoaServiceFactory) {
        List<ForkJoinTask<List<EE3DReportBean>>> daList = new ArrayList<>();
        DataManagementService ds = tcsoaServiceFactory.getDataManagementService();
        Folder dtL6Folder = getFolderByUid(dtL6EBOMFolderUid, ds);
        if (dtL6Folder != null) {
            try {
                daList.add(getDTProjectInfoParallel(ds, dtL6Folder, "Dell", dellProjects));
                daList.add(getDTProjectInfoParallel(ds, dtL6Folder, "HP", hpProjects));
                daList.add(getDTProjectInfoParallel(ds, dtL6Folder, "Lenovo", lenovoProjects));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("error", e);
            }
        } else {
            log.error("DT L6 EBOM not exist : " + dtL6EBOMFolderUid);
        }
        return daList;
    }


    public ForkJoinTask<List<EE3DReportBean>> getMntProjectInfoParallel(SessionService sessionService, TCSOAServiceFactory tcsoaServiceFactory,
                                                                        List<EE3DProjectBean> data) {
        if (forkJoinPool == null) {
            initForkJoinPool();
        }
        DataManagementService ds = tcsoaServiceFactory.getDataManagementService();
        return forkJoinPool.submit(() -> getReportListParallel(data.parallelStream().filter(e -> "MNT".equalsIgnoreCase(e.getBu())).map(e -> {
            try {
                String projectIds = "p" + e.getId();
                ModelObject[] modelObjects = QueryUtil.executeSOAQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Project_Folder",
                        new String[]{
                                "d9_SPAS_ID"},
                        new String[]{projectIds});
                //  object_name
                if (modelObjects != null && modelObjects.length > 0) {
                    Folder projectFolder = (Folder) modelObjects[0];
                    String projectName = getFolderName(projectFolder, ds);
                    EE3DReportBean bean = new EE3DReportBean();
                    BeanUtils.copyProperties(e, bean);
                    bean.setProjectSeries(e.getProjectSeries());
                    bean.setProjectName(projectName);
                    bean.setTempFolder(projectFolder);
                    return bean;
                }
            } catch (Exception e1) {
                e1.printStackTrace();

            }
            return null;
        }).filter(Objects::nonNull).map(e -> {
                    List<EE3DReportBean> beans = new ArrayList<>();
                    EE3DReportBean newBean = new EE3DReportBean();
                    BeanUtils.copyProperties(e, newBean);
                    Folder projectFolder = e.getTempFolder();
                    Folder eeFolder = findChildFolderByName(projectFolder, "EE", ds);
                    Folder psuFolder = findChildFolderByName(projectFolder, "PSU", ds);
                    e.setTempFolder(eeFolder);
                    e.setTempFolderName("EE Schematics(IF+Keypad)");
                    newBean.setTempFolder(psuFolder);
                    newBean.setTempFolderName("PI Schematics(PI)");
                    if (eeFolder != null) {
                        beans.add(e);
                    }
                    if (psuFolder != null) {
                        beans.add(newBean);
                    }
                    return beans;
                }
        ).flatMap(Collection::parallelStream).map(e -> getReportBeansByFolder(e, "phase", ds)).flatMap(Collection::parallelStream).map(e -> {
            List<EE3DReportBean> beans = new ArrayList<>();
            //Folder bomFolder = findChildFolderByName(e.getTempFolder(), e.getTempFolderName(), ds);
            //
            List<Folder[]> childList = new ArrayList<>();
            getFlatFolder(e.getTempFolder(), new Folder[2], childList, ds);
            if (childList.size() > 0) {
                String tempPhase = null;
                for (Folder[] childFolders : childList) {
                    Folder bomFolder = childFolders[childFolders.length - 1];
                    if (bomFolder != null) {
                        try {
                            String folderNames = appendFolderName(childFolders);
                            getProperty(ds, bomFolder, "contents");
                            WorkspaceObject[] contents = bomFolder.get_contents();
                            Set<ItemRevision> bomSet = new HashSet<>();
                            for (WorkspaceObject content : contents) {
                                if (content instanceof ItemRevision) {
                                    bomSet.add((ItemRevision) content);
                                }
                            }
                            if (bomSet.size() > 0 && (tempPhase == null || tempPhase.compareTo(e.getPhase()) <= 0)) {
                                tempPhase = e.getPhase();
                                ds.getProperties(bomSet.toArray(new ModelObject[0]), new String[]{"item_id"});
                                for (ItemRevision eebom : bomSet) {
                                    EE3DReportBean reportBean = new EE3DReportBean();
                                    BeanUtils.copyProperties(e, reportBean);
                                    reportBean.setVersion("/");
                                    reportBean.setBom(eebom);
                                    reportBean.setProjectName(e.getProjectName() + "/" + folderNames + eebom.get_item_id());
                                    beans.add(reportBean);
                                }
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }

            return beans;
        }).flatMap(Collection::parallelStream), sessionService, tcsoaServiceFactory));
    }


    String appendFolderName(Folder[] folders) {
        StringBuilder str = new StringBuilder();
        for (Folder folder : folders) {
            try {
                str.append(folder.get_object_name()).append("/");
            } catch (NotLoadedException e) {
                e.printStackTrace();
            }
        }
        return str.toString();
    }

    <T> boolean appendArrayValue(T[] array, T t) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                array[i] = t;
                return true;
            }
        }
        return false;
    }

    public void getFlatFolder(Folder parentFolder, Folder[] flatFolders, List<Folder[]> childList, DataManagementService ds) {
        Set<Folder> mFolders = findChildFolders(parentFolder, ds);
        ds.getProperties(mFolders.toArray(new ModelObject[0]), new String[]{"object_type"});
        mFolders.removeIf(mf -> !judgeType(mf, "Folder"));
        if (mFolders.size() == 0) {
            childList.add(flatFolders);
        }
        for (Folder childFolder : mFolders) {
            Folder[] childFlatFolders = ArrayUtil.clone(flatFolders);
            boolean bl = appendArrayValue(childFlatFolders, childFolder);
            if (childFlatFolders[childFlatFolders.length - 1] == null || !bl) {
                getFlatFolder(childFolder, childFlatFolders, childList, ds);
            } else {
                childList.add(childFlatFolders);
            }
        }
    }

    boolean judgeType(WorkspaceObject modelObject, String typeName) {
        try {
            return typeName.equalsIgnoreCase(modelObject.get_object_type());
        } catch (NotLoadedException notLoadedException) {
            notLoadedException.printStackTrace();
        }
        return false;
    }

    public ForkJoinTask<List<EE3DReportBean>> getMntProjectInfoParallel(SavedQueryService queryService, DataManagementService ds, String
            customer,
                                                                        String projects) {
        if (StringUtils.hasLength(projects)) {
            String[] projectInfoStrArray = projects.split(",");
            if (forkJoinPool == null) {
                initForkJoinPool();
            }
            return forkJoinPool.submit(() -> Stream.of(projectInfoStrArray).parallel().map(e -> {
                try {
                    String[] projectAndIds = e.split("/");
                    String projectSeries = projectAndIds[0];
                    String projectIds = projectAndIds[1];

                    // ppppp
                    ModelObject[] modelObjects = QueryUtil.executeSOAQuery(queryService, "__D9_Find_Project_Folder", new String[]{"d9_SPAS_ID"},
                            new String[]{projectIds});
                    if (modelObjects != null && modelObjects.length > 0) {
                        Folder projectFolder = (Folder) modelObjects[0];
                        String projectName = getFolderName(projectFolder, ds);
                        EE3DReportBean bean = new EE3DReportBean();
                        bean.setProjectSeries(projectSeries);
                        bean.setProjectName(projectName);
                        bean.setTempFolder(projectFolder);
                        return bean;
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();

                }
                return null;
            }).filter(Objects::nonNull).map(e -> {
                        List<EE3DReportBean> beans = new ArrayList<>();
                        EE3DReportBean newBean = new EE3DReportBean();
                        BeanUtils.copyProperties(e, newBean);
                        Folder projectFolder = e.getTempFolder();
                        Folder eeFolder = findChildFolderByName(projectFolder, "EE", ds);
                        Folder psuFolder = findChildFolderByName(projectFolder, "PSU", ds);
                        e.setTempFolder(eeFolder);
                        e.setTempFolderName("EE Schematics(IF+Keypad");
                        newBean.setTempFolder(psuFolder);
                        newBean.setTempFolderName("PI Schematics(PI)");
                        if (eeFolder != null) {
                            beans.add(e);
                        }
                        if (psuFolder != null) {
                            beans.add(newBean);
                        }
                        return beans;
                    }
            ).flatMap(Collection::parallelStream).map(e -> getReportBeansByFolder(e, "phase", ds)).flatMap(Collection::parallelStream).map(e -> {
                List<EE3DReportBean> beans = new ArrayList<>();
                Folder bomFolder = findChildFolderByName(e.getTempFolder(), e.getTempFolderName(), ds);
                getProperty(ds, bomFolder, "contents");
                try {
                    WorkspaceObject[] contents = bomFolder.get_contents();
                    for (WorkspaceObject content : contents) {
                        if (content instanceof ItemRevision) {
                            ItemRevision eebom = (ItemRevision) content;
                            EE3DReportBean reportBean = new EE3DReportBean();
                            BeanUtils.copyProperties(e, reportBean);
                            reportBean.setVersion("/");
                            reportBean.setBom(eebom);
                            reportBean.setCustomer(customer);
                            reportBean.setBu("MNT");
                            beans.add(reportBean);
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                return beans;
            }).flatMap(Collection::stream).collect(Collectors.toList()));
        }
        return null;
    }

    public List<EE3DReportBean> getMntProjectInfo(SavedQueryService queryService, DataManagementService ds, String customer, String projects) {
        List<EE3DReportBean> list = new ArrayList<>();
        String bu = "MNT";
        if (StringUtils.hasLength(projects)) {
            String[] projectInfoStrArray = projects.split(",");
            for (String projectInfoStr : projectInfoStrArray) {
                try {
                    String[] projectAndIds = projectInfoStr.split("/");
                    String projectSeries = projectAndIds[0];
                    String projectIds = projectAndIds[1];
                    ModelObject[] modelObjects = QueryUtil.executeSOAQuery(queryService, "__D9_Find_Project_Folder", new String[]{"d9_SPAS_ID"},
                            new String[]{projectIds});
                    if (modelObjects != null && modelObjects.length > 0) {
                        Folder projectFolder = (Folder) modelObjects[0];
                        String projectName = getFolderName(projectFolder, ds);
                        // ee layout
                        Folder eeFolder = findChildFolderByName(projectFolder, "EE", ds);
                        Folder psuFolder = findChildFolderByName(projectFolder, "Layout", ds);
                        List<EE3DReportBean> eeData = getMntProjectInfoByBUFolder(eeFolder, customer, ds, "EE Schematics(IF+Keypad)");
                        List<EE3DReportBean> psuData = getMntProjectInfoByBUFolder(psuFolder, customer, ds, "PI Schematics(PI)");
                        list.addAll(eeData);
                        list.addAll(psuData);
                        for (EE3DReportBean reportBean : list) {
                            reportBean.setVersion("/");
                            reportBean.setBu(bu);
                            reportBean.setProjectName(projectName);
                            reportBean.setProjectSeries(projectSeries);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("error", e);
                }
            }
        }
        return list;
    }

    public List<EE3DReportBean> getMntProjectInfoByBUFolder(Folder buFolder, String customer, DataManagementService ds, String bomFolderName) throws
            Exception {
        List<EE3DReportBean> list = new ArrayList<>();
        Set<Folder> phaseFolderSet = findChildFolders(buFolder, ds);
        for (Folder phaseFolder : phaseFolderSet) {
            String phaseStr = getFolderName(phaseFolder, ds);
            Folder bomFolder = findChildFolderByName(phaseFolder, bomFolderName, ds); //
            if (bomFolder != null) {
                getProperty(ds, bomFolder, "contents");
                WorkspaceObject[] contents = bomFolder.get_contents();
                for (WorkspaceObject content : contents) {
                    if (content instanceof ItemRevision) {
                        ItemRevision eebom = (ItemRevision) content;
                        EE3DReportBean reportBean = new EE3DReportBean();
                        reportBean.setPhase(phaseStr);
                        reportBean.setBom(eebom);
                        reportBean.setCustomer(customer);
                        list.add(reportBean);
                    }
                }
            }
        }
        return list;
    }

    public List<EE3DReportBean> getMntProjectInfoByBUFolder(Folder buFolder, String customer, DataManagementService ds) throws Exception {
        List<EE3DReportBean> list = new ArrayList<>();
        Set<Folder> phaseFolderSet = findChildFolders(buFolder, ds);
        for (Folder phaseFolder : phaseFolderSet) {
            String phaseStr = getFolderName(phaseFolder, ds);
            Set<Folder> machineFolderSet = findChildFolders(phaseFolder, ds); //
            //machineFolderSet  普通文件夹
            ds.getProperties(machineFolderSet.toArray(new ModelObject[0]), new String[]{"object_type"});
            for (Folder machineFolder : machineFolderSet) {
                String folderType = machineFolder.get_object_type();
                if ("folder".equalsIgnoreCase(folderType)) {
                    Set<Folder> clsTypeFolderSet = findChildFolders(machineFolder, ds);
                    for (Folder clsTypeFolder : clsTypeFolderSet) {
                        getProperty(ds, clsTypeFolder, "contents");
                        WorkspaceObject[] contents = clsTypeFolder.get_contents();
                        for (WorkspaceObject content : contents) {
                            if (content instanceof ItemRevision) {
                                ItemRevision eebom = (ItemRevision) content;
                                EE3DReportBean reportBean = new EE3DReportBean();
                                reportBean.setPhase(phaseStr);
                                reportBean.setBom(eebom);
                                reportBean.setCustomer(customer);
                                list.add(reportBean);
                            }
                        }
                    }
                }
            }
        }
        return list;
    }


    public List<EE3DReportBean> getReportBeansByFolder(EE3DReportBean parentBean, String fieldName, DataManagementService ds) {
        List<EE3DReportBean> tempList = new ArrayList<>();
        if (parentBean != null && parentBean.getTempFolder() != null) {
            Set<Folder> folders = findChildFolders(parentBean.getTempFolder(), ds);
            if (folders.size() > 0) {
                ds.getProperties(folders.toArray(new ModelObject[0]), new String[]{"object_name", "creation_date"});
                for (Folder childFolder : folders) {
                    String childFolderName = getFolderName(childFolder, ds);
                    if (childFolderName != null) {
                        EE3DReportBean bean = new EE3DReportBean();
                        BeanUtils.copyProperties(parentBean, bean);
                        bean.setTempFolder(childFolder);
                        PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(EE3DReportBean.class, fieldName);
                        if (propertyDescriptor != null) {
                            try {
                                Method method = propertyDescriptor.getWriteMethod();
                                method.invoke(bean, childFolderName);
                                tempList.add(bean);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return tempList;
    }

    EE3DReportBean getByFolderLastCreateTime(List<EE3DReportBean> list) {
        if (list == null || list.size() == 0) return null;
        list.sort(Comparator.comparing(e -> {
            try {
                return e.getTempFolder().get_creation_date().getTime();
            } catch (NotLoadedException notLoadedException) {
                notLoadedException.printStackTrace();
            }
            return new Date(0);
        }));
        return list.get(list.size() - 1);
    }


    public ForkJoinTask<List<EE3DReportBean>> getDTProjectInfoParallel(SessionService sessionService, TCSOAServiceFactory tcsoaServiceFactory,
                                                                       List<EE3DProjectBean> projectList) {
        try {
            DataManagementService ds = tcsoaServiceFactory.getDataManagementService();
            return forkJoinPool.submit(() -> getReportListParallel(projectList.stream().filter(e -> "DT".equalsIgnoreCase(e.getBu())).map(e -> {
                EE3DReportBean reportBean = new EE3DReportBean();
                BeanUtils.copyProperties(e, reportBean);
                Folder projectFolder = getFolderByUid(e.getId(), ds);
                reportBean.setTempFolder(projectFolder);
                return reportBean;
            }).map(e -> getReportBeansByFolder(e, "phase", ds)).map(this::getByFolderLastCreateTime)
                    .map(e -> getReportBeansByFolder(e, "version", ds)).map(this::getByFolderLastCreateTime)
                    .map(e -> {
                        List<EE3DReportBean> beanList = new ArrayList<>();
                        try {
                            if (e != null) {
                                Folder revFolder = e.getTempFolder();
                                getProperty(ds, revFolder, "contents");
                                WorkspaceObject[] contents = revFolder.get_contents();
                                for (WorkspaceObject content : contents) {
                                    if (content instanceof ItemRevision) {
                                        ItemRevision eebom = (ItemRevision) content;
                                        EE3DReportBean reportBean = new EE3DReportBean();
                                        BeanUtils.copyProperties(e, reportBean);
                                        reportBean.setBu("DT");
                                        reportBean.setBom(eebom);
                                        beanList.add(reportBean);
                                    }
                                }
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        return beanList;
                    }).flatMap(Collection::parallelStream), sessionService, tcsoaServiceFactory));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error", e);
        }
        return null;
    }


    public ForkJoinTask<List<EE3DReportBean>> getDTProjectInfoParallel(DataManagementService ds, Folder dtL6Folder, String customer,
                                                                       String projects) {
        try {
            if (StringUtils.hasLength(projects)) {
                String[] projectArray = projects.split(",");
                if (forkJoinPool == null) {
                    initForkJoinPool();
                }
                return forkJoinPool.submit(() -> Stream.of(projectArray).parallel().map(e -> {
                    Folder seriesFolder = findChildFolderByName(dtL6Folder, e, ds);
                    if (seriesFolder != null) {
                        EE3DReportBean rootBean = new EE3DReportBean();
                        rootBean.setProjectSeries(e);
                        rootBean.setTempFolder(seriesFolder);
                        return rootBean;
                    }
                    return null;
                }).filter(Objects::nonNull).map(e -> getReportBeansByFolder(e, "projectName", ds)).flatMap(Collection::parallelStream)
                        .map(e -> getReportBeansByFolder(e, "phase", ds)).flatMap(Collection::parallelStream)
                        .map(e -> getReportBeansByFolder(e, "version", ds)).flatMap(Collection::parallelStream)
                        .map(e -> {
                            List<EE3DReportBean> beanList = new ArrayList<>();
                            try {
                                Folder revFolder = e.getTempFolder();
                                getProperty(ds, revFolder, "contents");
                                WorkspaceObject[] contents = revFolder.get_contents();
                                for (WorkspaceObject content : contents) {
                                    if (content instanceof ItemRevision) {
                                        ItemRevision eebom = (ItemRevision) content;
                                        EE3DReportBean reportBean = new EE3DReportBean();
                                        BeanUtils.copyProperties(e, reportBean);
                                        reportBean.setBu("DT");
                                        reportBean.setBom(eebom);
                                        reportBean.setCustomer(customer);
                                        beanList.add(reportBean);
                                    }
                                }
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                            return beanList;
                        }).flatMap(Collection::stream).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error", e);
        }
        return null;
    }


    public List<EE3DReportBean> getProjectInfo(DataManagementService ds, Folder dtL6Folder, String customer, String projects) {
        List<EE3DReportBean> list = new ArrayList<>();
        String bu = "DT";
        try {
            if (StringUtils.hasLength(projects)) {
                String[] projectArray = projects.split(",");
                for (String projectSeries : projectArray) {
                    Folder projectSeriesFolder = findChildFolderByName(dtL6Folder, projectSeries, ds);
                    if (projectSeriesFolder != null) {
                        Set<Folder> projectFolders = findChildFolders(projectSeriesFolder, ds);
                        for (Folder projectFolder : projectFolders) {
                            String projectName = getFolderName(projectFolder, ds);
                            Set<Folder> phaseFolders = findChildFolders(projectFolder, ds);
                            for (Folder phaseFolder : phaseFolders) {
                                String phase = getFolderName(phaseFolder, ds);
                                Set<Folder> revFolders = findChildFolders(phaseFolder, ds);
                                for (Folder revFolder : revFolders) {
                                    String rev = getFolderName(revFolder, ds);
                                    ds.refreshObjects(new ModelObject[]{revFolder});
                                    getProperty(ds, revFolder, "contents");
                                    WorkspaceObject[] contents = revFolder.get_contents();
                                    for (WorkspaceObject content : contents) {
                                        if (content instanceof ItemRevision) {
                                            ItemRevision eebom = (ItemRevision) content;
                                            EE3DReportBean reportBean = new EE3DReportBean();
                                            reportBean.setBu(bu);
                                            reportBean.setProjectSeries(projectSeries);
                                            reportBean.setProjectName(projectName);
                                            reportBean.setPhase(phase);
                                            reportBean.setVersion(rev);
                                            reportBean.setBom(eebom);
                                            reportBean.setCustomer(customer);
                                            list.add(reportBean);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error", e);
        }
        return list;
    }


    public List<Item3DInfo> getItem3DList_(Set<ItemRevision> itemRevs, DataManagementService ds) {
        System.out.println("data source ------ >> " + DynamicDataSourceContextHolder.getDataSourceType());
        List<Item3DInfo> list3ds = new ArrayList<>();
        try {
            for (ItemRevision itemRevision : itemRevs) {
                // String category = itemRevision.get_ics_subclass_name();
                String partPn = itemRevision.get_item_id();
                String partType = itemRevision.getPropertyObject("d9_PartType").getStringValue();
                if (!StringUtils.hasLength(partType)) {
                    partType = cisMapper.getCISPartType(sqlPn(partPn));
                }
                if (StringUtils.hasLength(partType)) {
                    partType = getPartType(partType);
                    if (StringUtils.hasLength(partType) && filterCISPart(partPn)) {
                        String mfgPn = itemRevision.getPropertyObject("d9_ManufacturerPN").getStringValue();
                        //String category = partType;
                        //if (partType.toUpperCase().startsWith("Connector\\".toUpperCase())) {
                        String[] tempStrs = partType.split("\\\\");
                        String category = tempStrs[0];
                        partType = tempStrs[1];
                        // }
                        Item3DInfo itemInfo = new Item3DInfo();
                        itemInfo.has3D = has3D(partPn);
                        itemInfo.partPn = partPn;
                        itemInfo.category = category;
                        itemInfo.partType = partType;
                        itemInfo.mfgPn = mfgPn;
                        list3ds.add(itemInfo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error", e);
        } finally {
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
        return list3ds;
    }

    public List<Item3DInfo> getItem3DList_Dell(Set<ItemRevision> itemRevs, DataManagementService ds) throws NotLoadedException {
        DynamicDataSourceContextHolder.setDataSourceType(DataSourceType.CISDELL.name());
        return getItem3DList_(itemRevs, ds);
    }

    public List<Item3DInfo> getItem3DList(Set<ItemRevision> itemRevs, DataManagementService ds) {
        DynamicDataSourceContextHolder.setDataSourceType(DataSourceType.CIS.name());
        return getItem3DList_(itemRevs, ds);
    }

    public String getCisLibrary(String bu, String customer) {
        if ("DT".equalsIgnoreCase(bu) && "dell".equalsIgnoreCase(customer)) {
            return "CIS Dell library";
        } else {
            return "CIS Common library";
        }
    }

    public List<EE3DReportBean> getItemsClsData(Set<ItemRevision> itemRevs, String customer, String bu, DataManagementService ds) throws
            NotLoadedException {
        List<EE3DReportBean> list = new ArrayList<>();
        ds.getProperties(itemRevs.toArray(new ModelObject[0]), new String[]{"d9_PartType", "item_id", "d9_ManufacturerPN", "object_type"});
        itemRevs.removeIf(e -> {
            try {
                return "D9_VirtualPart".equalsIgnoreCase(e.get_object_type());
            } catch (NotLoadedException notLoadedException) {
                notLoadedException.printStackTrace();
            }
            return false;
        });
        List<Item3DInfo> list3ds = null;
        if ("DT".equalsIgnoreCase(bu) && "dell".equalsIgnoreCase(customer)) {
            list3ds = getItem3DList_Dell(itemRevs, ds);
            list3ds.removeIf(e -> "jumper&shunt".equalsIgnoreCase(e.partType));
        } else {
            list3ds = getItem3DList(itemRevs, ds);
        }
        if (list3ds.size() > 0) {
            Map<String, List<Item3DInfo>> listGroup = list3ds.stream().collect(Collectors.groupingBy(e -> e.category + e.partType,
                    Collectors.toList()));
            for (List<Item3DInfo> list3dsGroup : listGroup.values()) {
                if (list3dsGroup.size() > 0) {
                    EE3DReportBean reportBean = new EE3DReportBean();
                    reportBean.setCategory(list3dsGroup.get(0).category);
                    reportBean.setPartType(list3dsGroup.get(0).partType);
                    reportBean.setPartCount(list3dsGroup.size());
                    int count3d = (int) list3dsGroup.stream().filter(e -> e.has3D).count();
                    Set<String> noCisModel =
                            list3dsGroup.stream().filter(e -> !e.has3D).map(e -> e.mfgPn + "$" + e.partPn).collect(Collectors.toSet());
                    reportBean.setNoCisModel(noCisModel);
                    reportBean.setPart3DCount(count3d);
                    reportBean.setCustomer(customer);
                    list.add(reportBean);
                }
            }
        }
        return list;
    }


    public String getPartType(String partType) {
        for (String partType_ : PART_TYPES) {
            if (partType.toUpperCase().startsWith(partType_.toUpperCase())) {
                return partType;
            }
        }
        return null;
    }

    public boolean filterCISPart(String pn) {
        return !pn.startsWith("T-PDM");
    }

    public Set<ItemRevision> getBOMItems(ItemRevision topRev, StructureManagementService structureManagementService, SessionService
            sessionService) throws Exception {
        Set<ItemRevision> itemRevs = new HashSet<>();
        List bomWindowParentLine = StructureManagementUtil.openBOMWindow(structureManagementService, topRev);
        if (bomWindowParentLine != null && bomWindowParentLine.size() == 2) {
            BOMWindow bomWindow = (BOMWindow) bomWindowParentLine.get(0);
            BOMLine topLine = (BOMLine) bomWindowParentLine.get(1);
            Map<String, BOMLine> childs = StructureManagementUtil.expandPSEAllLevels(structureManagementService, topLine, sessionService);
            for (BOMLine childline : childs.values()) {
                if (!childline.get_fnd0bl_is_substitute()) {
                    ItemRevision childRev = (ItemRevision) childline.get_bl_revision();
                    itemRevs.add(childRev);
                }
            }
            StructureManagementUtil.closeBOMWindow(structureManagementService, bomWindow);
        }
        return itemRevs;

    }

    public boolean has3D(String hhpn) {
        int count = cisMapper.getThreeDDrawingRecordCount(sqlPn(hhpn));
        return count > 0;
    }

    public String sqlPn(String hhpn) {
        if (hhpn.endsWith("-G") || hhpn.endsWith("-H")) {
            return hhpn.substring(0, hhpn.length() - 1) + "_";
        }
        return hhpn;
    }

    static class Item3DInfo {
        String mfgPn;
        String partPn;
        String partType;
        String category;
        boolean has3D = false;
    }


    Folder findChildFoldersLastTime(Folder parentFolder, DataManagementService dataManagementService) {
        try {
            List<Folder> childFolders = new ArrayList<>();
            getProperty(dataManagementService, parentFolder, "contents");
            WorkspaceObject[] contents = parentFolder.get_contents();
            dataManagementService.getProperties(contents, new String[]{"creation_date"});
            for (WorkspaceObject content : contents) {
                if (content instanceof Folder) {
                    Folder folder = (Folder) content;
                    childFolders.add(folder);
                }
            }
            childFolders.sort(Comparator.comparing(e -> {
                try {
                    return e.get_creation_date().getTime();
                } catch (NotLoadedException notLoadedException) {
                    notLoadedException.printStackTrace();
                }
                return new Date(0);
            }));
            return childFolders.get(childFolders.size() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    Set<Folder> findChildFolders(Folder parentFolder, DataManagementService dataManagementService) {
        Set<Folder> childFolders = new HashSet<>();
        try {
            getProperty(dataManagementService, parentFolder, "contents");
            WorkspaceObject[] contents = parentFolder.get_contents();
            for (WorkspaceObject content : contents) {
                if (content instanceof Folder) {
                    Folder folder = (Folder) content;
                    childFolders.add(folder);
                }
            }
            if (childFolders.size() > 0) {
                ModelObject[] modelObjects = childFolders.toArray(new ModelObject[0]);
                dataManagementService.refreshObjects(modelObjects);
            }
            return childFolders;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return childFolders;
    }


    Map<String, Folder> findChildFolderByNames(Folder parentFolder, Set<String> folderNames, DataManagementService ds) {
        Map<String, Folder> folderMap = new HashMap<>();
        try {
            //ds.refreshObjects(new ModelObject[]{parentFolder});
            getProperty(ds, parentFolder, "contents");
            WorkspaceObject[] contents = parentFolder.get_contents();
            for (WorkspaceObject content : contents) {
                if (content instanceof Folder) {
                    Folder folder = (Folder) content;
                    String folderNameTemp = getFolderName(folder, ds);
                    if (folderNames.contains(folderNameTemp)) {
                        folderMap.put(folderNameTemp, folder);
                    }
                }
                if (folderMap.size() == folderNames.size()) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return folderMap;
    }

    Folder findChildFolderByName(Folder parentFolder, String folderName, DataManagementService ds) {
        try {
            String childUid = tcService.getChildFolderUidByName(parentFolder.getUid(), folderName);
            if (StringUtils.hasLength(childUid)) {
                return TCUtils.findFolderByUid(ds, childUid);
            }
            return null;
            //ds.refreshObjects(new ModelObject[]{parentFolder});
//            getProperty(ds, parentFolder, "contents");
//            WorkspaceObject[] contents = parentFolder.get_contents();
//            for (WorkspaceObject content : contents) {
//                if (content instanceof Folder) {
//                    Folder folder = (Folder) content;
//                    String folderNameTemp = getFolderName(folder, ds);
//                    if (folderNameTemp.equals(folderName)) {
//                        return folder;
//                    }
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void getProperty(DataManagementService dmService, ModelObject object, String propName) {
        if (object != null && !ArrayUtil.contains(object.getPropertyNames(), propName)) {
            dmService.getProperties(new ModelObject[]{object}, new String[]{propName});
        }
    }

    public String getFolderName(Folder folder, DataManagementService ds) {
        try {
            getProperty(ds, folder, "object_name");
            return folder.get_object_name();

        } catch (NotLoadedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void loadFolderProp(SessionService sessionService) {
        TCUtils.setDefaultLoadProperty(sessionService, "Folder", new String[]{"contents", "object_name"});
        TCUtils.setDefaultLoadProperty(sessionService, "D9_WorkAreaFolder", new String[]{"contents", "object_name"});
        TCUtils.setDefaultLoadProperty(sessionService, "D9_FunctionFolder", new String[]{"contents", "object_name"});
    }

    List<EE3DCISModelInfo> getNoCisInfoList2() {
//        List<EE3DProjectBean> projectData = new ArrayList<>();
//        List<EE3DProjectBean> dtList = getDTProjectList();
//        List<EE3DProjectBean> mntList = getMNTProjectList(mntProjectIds);
//        projectData.addAll(mntList);
//        projectData.addAll(dtList);
//        List<EE3DReportBean> dataList = getAllReport(projectData);
        List<EE3DReportBean> dataList = tcService.selectEE3DReportList(null);
        System.out.println(dataList.size());
        Map<String, Set<String>> noCisMap = dataList.stream().filter(e -> e.getNoCisModel() != null && e.getNoCisModel().size() > 0).map(e -> {
            Map<String, Set<String>> map = new HashMap<>(1);
            Set<String> nSet = e.getNoCisModel().stream().map(e1 -> e1.split("\\$")[0]).collect(Collectors.toSet());
            map.put(e.getBu() + "#" + e.getCustomer(), nSet);
            return map.entrySet();
        }).flatMap(Collection::stream).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> {
            v1.addAll(v2);
            return v1;
        }));
        List<EE3DCISModelInfo> noCisInfoList = new ArrayList<>();
        noCisMap.forEach((k, v) -> {
            String[] buCustomer = k.split("#");
            noCisInfoList.addAll(getNoCISModelInfos(v, buCustomer[0], buCustomer[1]));
        });
        System.out.println(noCisInfoList.size());
        return noCisInfoList;
    }

    List<EE3DCISModelInfo> getNoCisInfoList() {
        List<EE3DCISModelInfo> noCisInfoList = new ArrayList<>();
        List<EE3DReportBean> dataList = tcService.selectEE3DReportList(null);
        for (EE3DReportBean ee3DReportBean : dataList) {
            Set<String> noCisPns = ee3DReportBean.getNoCisModel();
            if (noCisPns != null && noCisPns.size() > 0) {
                List<EE3DCISModelInfo> noCisList = getNoCISModelInfos(noCisPns, ee3DReportBean.getBu(), ee3DReportBean.getCustomer());
                for (EE3DCISModelInfo ee3DCISModelInfo : noCisList) {
                    ee3DCISModelInfo.setTcCustomer(ee3DReportBean.getCustomer());
                    ee3DCISModelInfo.setTcProjectSeries(ee3DReportBean.getProjectSeries());
                    noCisInfoList.add(ee3DCISModelInfo);
                }
            }
        }
        return noCisInfoList;
    }


    public Map<EE3DUserPojo, Set<EE3DCISModelInfo>> getCISDataMail(List<EE3DCISModelInfo> dataList) {
        Map<String, String> roleMap = ee3DWFInfoList.stream().collect(Collectors.toMap(e -> e.getWfName() + e.getWorkItem(), EE3DWFInfo::getFunc,
                (v1, v2) -> v1));
        Function<EE3DUserPojo, keyInfo> userF = k -> {
            keyInfo keyInfo = new keyInfo();
            keyInfo.bu = k.getBu();
            keyInfo.customer = k.getCustomer();
            keyInfo.role = k.getFunc();
            return keyInfo;
        };
        Function<EE3DCISModelInfo, keyInfo> userD = k -> {
            keyInfo keyInfo = new keyInfo();
            keyInfo.customer = k.getCisCustomer();
            keyInfo.bu = k.getProductLine();
            if (StringUtils.hasLength(keyInfo.bu) && StringUtils.hasLength(k.getProcessName()) && StringUtils.hasLength(k.getProcessStatus())) {
                keyInfo.role = roleMap.get(k.getProcessName() + k.getProcessStatus());
                if (keyInfo.role == null) {
                    log.info("can not get excel wf sheet role :  hhpn:" + k.getHhPn() + "  mfgpn:" + k.getMfg() + "  processName:" + k.getProcessName() + "  processStatus:" + k.getProcessStatus());
                }
            }
            return keyInfo;
        };
        Map<EE3DUserPojo, keyInfo> userInfoMapCustomer = listToKeyMap(ee3dUserList, userF);
        Map<EE3DCISModelInfo, keyInfo> dataMap = listToKeyMap(dataList, userD);
        Map<EE3DUserPojo, Set<EE3DCISModelInfo>> result = new HashMap<>();
        userInfoMapCustomer.forEach((k, v) -> {
            Set<EE3DCISModelInfo> userDataList = new HashSet<>();
            dataMap.forEach((k1, v1) -> {
                if (v.role.equalsIgnoreCase(v1.role) && v.customer.equalsIgnoreCase(v1.customer) && v.bu.equalsIgnoreCase(v1.bu)) {
                    userDataList.add(k1);
                } else if (!StringUtils.hasLength(v1.customer) && v.role.equalsIgnoreCase(v1.role) && v.bu.equalsIgnoreCase(v1.bu)) {
                    userDataList.add(k1);
                } else if (!StringUtils.hasLength(v1.bu) && v.role.equalsIgnoreCase("CE")) {
                    userDataList.add(k1);
                } else if (!StringUtils.hasLength(v1.role) && StringUtils.hasLength(v1.bu) && v.role.equalsIgnoreCase("CE") && v.bu.equalsIgnoreCase(v1.bu)) {
                    userDataList.add(k1);
                }
            });
            result.put(k, userDataList);
        });
        return result;
    }

    public <T> Map<T, keyInfo> listToKeyMap(List<T> dataList, Function<T, keyInfo> keyFunc) {
        return dataList.stream().collect(Collectors.toMap(k -> k, keyFunc, (o1, o2) -> o1));
    }

    public <T> Map<keyInfo, List<T>> listToMap(List<T> dataList, Function<T, keyInfo> keyFunc) {
        return dataList.stream().collect(Collectors.toMap(keyFunc, v -> {
            List<T> list = new ArrayList<>();
            list.add(v);
            return list;
        }, (v1, v2) -> {
            v1.addAll(v2);
            return v1;
        }));
    }

    /**
     * 1. 定時郵件通知每週一早上6點發送給相應的部門負責人
     * 2. 郵件通知人設置：
     * 2.1.Process Name & Process Status & Department 欄位不為空情況：發送給部門負責人前，依據Product line / Department / 根據Process Name & Process Status mapping
     * "CIS節點“Sheet頁的 ”工作流“ & ”節點名稱“，獲取責任部門，匯總待辦信息後發送給責任部門的負責人郵箱
     * 2.2. Process Name = “Standard Process”， Process Status = “Release”，郵件統一發給Product line的CE
     * 2.3. Process Name & Process Status != null && Department = null,根據Process Name & Process Status mapping  "CIS節點“Sheet頁的 ”工作流“ &
     * ”節點名稱“，獲取責任部門，匯總待辦信息後發送給責任部門的負責人郵箱
     */
    @XxlJob("NOCisMailSchedule")
    public void noCisMailSchedule() {
        XxlJobHelper.log("start noCisMailSchedule");
        List<EE3DCISModelInfo> dataList = getNoCisInfoList();
        dataList.removeIf(e -> StrUtil.isEmpty(e.getStandardPn()) || e.getStandardPn().startsWith("T-PDM"));
        Map<EE3DUserPojo, Set<EE3DCISModelInfo>> userDataMap = getCISDataMail(dataList);
        userDataMap.forEach((u, d) -> {
            if (d.size() > 0) {
                try {
                    String action = getMailAction(u.getFunc());
                    String subject = "请登录PDMCIS系统完成3D模型上传相关操作" + action;
                    String dataContent = tableHtml(d);
                    String mailContent = "<html><head><style>div{margin:10px;}table{margin:20px;border-spacing: 0px;border-collapse:collapse}" +
                            "th{border:solid 1px " +
                            "#000000;" +
                            "height: 35px;padding:6px;background-color: #99ffff;} td{border:solid 1px #000000;" +
                            "height: 35px;padding:5px;}</style></head><body><div>Dear " + u.getName() + ",</div> " +
                            "<br>&nbsp;&nbsp;&nbsp;&nbsp;请通知任务负责人，登录PDMCIS系统完成3D模型上传相关操作" + action +
                            "，RDCOE" +
                            "将在周四汇总统计，周五早会向RD各高阶主管汇报，谢谢！" + dataContent + "<b>Teamcenter 系统自动定时发送,请勿回复邮件！ </b><br></body></html>";

                    JSONObject httpmap = new JSONObject();
                    httpmap.put("sendTo", u.getMailAddr());
                    //httpmap.put("sendTo", "robert.y.peng@foxconn.com");
                    httpmap.put("sendCc", "ivy.xw.cheng@foxconn.com");
                    httpmap.put("subject", subject);
                    httpmap.put("htmlmsg", mailContent);
                    tcMailClient.sendMail3Method(httpmap.toJSONString());
                } catch (Exception e) {
                    e.printStackTrace();
                    XxlJobHelper.log(e);
                }
            }
        });
        XxlJobHelper.handleSuccess();
    }

    @XxlJob("EE3DReportToDBSchedule")
    public void ee3DReportToDBSchedule() {
        XxlJobHelper.log("start EE3DReportToDB Schedule");
        List<EE3DReportBean> dataList = getAllReportNew();
        XxlJobHelper.log(" EE3DReport  tc data  size : " + dataList.size());
        writeTempReportDB(dataList);

    }

    @Transactional
    public void writeTempReportDB(List<EE3DReportBean> dataList) {
        tcService.deleteAllEE3DReport();
        XxlJobHelper.log(" EE3DReportToDB   delete all db data ");
        int insertSize = tcService.batchInsertEE3DReport(dataList);
        XxlJobHelper.log(" EE3DReport insert DB data  size : " + insertSize);
    }

    String tableHtml(Set<EE3DCISModelInfo> dataList) {
        StringBuilder tableHtml = new StringBuilder();
        tableHtml.append("<table>");
        tableHtml.append("<thead><tr>");
        tableHtml.append("<th>No.</th><th>Customer</th><th>Project Series</th><th>Product Line</th><th>Department</th><th>HHPN</th><th>Standard " +
                "Pn</th><th>MFG</th><th>MFG Pn</th><th>Part " +
                "Type</th><th>Process" +
                " Name</th><th>Process Status</th><th" +
                ">CIS Customer</th><th>Remark</th>");
        tableHtml.append("</tr></thead><tbody>");
        int i = 0;
        for (EE3DCISModelInfo info : dataList) {
            i++;
            tableHtml.append("<tr>");
            tableHtml.append("<td>").append(i).append("</td>");
            tableHtml.append("<td>").append(info.getTcCustomer()).append("</td>");
            tableHtml.append("<td>").append(info.getTcProjectSeries()).append("</td>");
            tableHtml.append("<td>").append(info.getProductLine()).append("</td>");
            tableHtml.append("<td>").append(replaceNull(info.getDepartment())).append("</td>");
            tableHtml.append("<td>").append(info.getHhPn()).append("</td>");
            tableHtml.append("<td>").append(info.getStandardPn()).append("</td>");
            tableHtml.append("<td>").append(info.getMfg()).append("</td>");
            tableHtml.append("<td><a href='").append(info.getUrl()).append("'>").append(info.getMfgPn()).append("</a></td>");
            tableHtml.append("<td>").append(info.getPartType()).append("</td>");
            tableHtml.append("<td>").append(info.getProcessName()).append("</td>");
            tableHtml.append("<td>").append(info.getProcessStatus()).append("</td>");
            tableHtml.append("<td>").append(replaceNull(info.getCisCustomer())).append("</td>");
            tableHtml.append("<td>").append(info.getReMark()).append("</td>");
            tableHtml.append("</tr>");
        }
        tableHtml.append("</tbody></table>");
        return tableHtml.toString();
    }

    String replaceNull(String str) {
        return str == null ? "/" : str;
    }

    String getMailAction(String roleName) {
        String action = "";
        switch (roleName.toUpperCase()) {
            case "LAYOUT":
                action = "（DXF文件上传）";
                break;
            case "SD":
            case "ME":
                action = "（3D模型上传）";
                break;
            case "CE":
                action = "（3D原始模型上传）";
                break;
            default:
                break;
        }
        return action;
    }

    public void connectAgile() {
        Map<Integer, String> params = new HashMap<>();
        try {
            AgileSessionFactory factory = AgileSessionFactory.getInstance(agileUrl);
            params.put(AgileSessionFactory.PASSWORD, agilePw);
            params.put(AgileSessionFactory.USERNAME, agileUser);
            agileSession = factory.createSession(params);
            log.info(agileUrl + " connect agile success !");
        } catch (APIException e) {
            e.printStackTrace();
        }
    }


    public List<EE3DReportBean> getDTReportBeanListByPCA(SessionService sessionService, TCSOAServiceFactory tcsoaServiceFactory,
                                                         String pcaPns) {
        if (StringUtils.hasLength(pcaPns)) {
            connectAgile();
            List<EE3DReportBean> list = Stream.of(pcaPns.split(",")).map(pcaPn -> {
                try {
                    EE3DReportBean baseBean = getDTReportBeanByPCA(tcsoaServiceFactory.getSavedQueryService(),
                            tcsoaServiceFactory.getDataManagementService(), pcaPn);
                    baseBean = fillBOMToItems(baseBean, sessionService, tcsoaServiceFactory);


                    List<EE3DReportBean> resultList = convertBomToBean(baseBean, tcsoaServiceFactory);
                    return resultList;
                } catch (Exception e) {
                    log.error(e);
                    e.printStackTrace();
                }
                return null;
            }).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
            agileSession.close();
            return list;
        }
        return null;
    }


    public EE3DReportBean getDTReportBeanByPCA(SavedQueryService queryService, DataManagementService dataManagementService, String pcaPn) throws Exception {
        Item pcaItem = TCUtils.queryItemByIDOrName(queryService, dataManagementService, pcaPn, "");
        ItemRevision pcaRev = ItemUtil.getItemLatestRevision(dataManagementService, pcaItem);
        List<List<WorkspaceObject>> list = getParentFolder(dataManagementService, pcaRev, 5);
        list.removeIf(e -> !(e.get(e.size() - 1).getUid()).equalsIgnoreCase(dtL6EBOMFolderUid));
        if (list.size() == 1) {
            EE3DReportBean ee3DReportBean = new EE3DReportBean();
            List<WorkspaceObject> folderList = list.get(0);
            ee3DReportBean.setBu("DT");
            ee3DReportBean.setBom(pcaRev);
            ee3DReportBean.setVersion(getFolderName((Folder) folderList.get(1), dataManagementService));
            ee3DReportBean.setPhase(getFolderName((Folder) folderList.get(2), dataManagementService));
            ee3DReportBean.setProjectName(getFolderName((Folder) folderList.get(3), dataManagementService));
            ee3DReportBean.setProjectSeries(getFolderName((Folder) folderList.get(4), dataManagementService));
            ee3DReportBean.setCustomer(getAgileCustomer(pcaPn));
            return ee3DReportBean;
        } else {
            log.error(pcaPn + "  tc data error  !");
            throw new RuntimeException(pcaPn + " pca bom  tc data error  !");
        }
    }

    String getAgileCustomer(String pcaPn) throws APIException {
        IItem item = (IItem) agileSession.getObject(IItem.OBJECT_TYPE, pcaPn);
        return item.getValue(1004).toString();
    }

    public List<List<WorkspaceObject>> getParentFolder(DataManagementService dataManagementService, WorkspaceObject tcObject,
                                                       int level) {
        List<WorkspaceObject> tcObjectList = new ArrayList<>();
        tcObjectList.add(tcObject);
        List<List<WorkspaceObject>> list = new ArrayList<>();
        getParentFolder_(dataManagementService, tcObjectList, list, level);
        return list;
    }

    void getParentFolder_(DataManagementService dataManagementService, List<WorkspaceObject> tcObjectList,
                          List<List<WorkspaceObject>> parentTCObjectList, int level) {
        WorkspaceObject tcObject = tcObjectList.get(tcObjectList.size() - 1);
        com.teamcenter.services.strong.core._2007_01.DataManagement.WhereReferencedResponse resp =
                dataManagementService.whereReferenced(new WorkspaceObject[]{tcObject}, 1);
        ServiceData sd = resp.serviceData;
        level--;
        for (int i = 0; i < sd.sizeOfPlainObjects(); i++) {
            ModelObject parentTCObject = sd.getPlainObject(i);
            if (parentTCObject instanceof Folder) {
                List<WorkspaceObject> newTcObjectList = new ArrayList<>(tcObjectList);
                newTcObjectList.add((WorkspaceObject) parentTCObject);
                if (level > 0) {
                    getParentFolder_(dataManagementService, newTcObjectList, parentTCObjectList, level);
                } else {
                    parentTCObjectList.add(newTcObjectList);
                }
            }
        }
    }


    public static class keyInfo {
        String bu;
        String customer;
        String role;

    }
}
