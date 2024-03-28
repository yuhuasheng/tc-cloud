package com.foxconn.plm.integrate.cis.service.Impl;

import cn.hutool.core.util.ArrayUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.integrate.cis.domain.EE3DProjectBean;
import com.foxconn.plm.integrate.cis.domain.EE3DReportBean;
import com.foxconn.plm.integrate.cis.mapper.cis.CISMapper;
import com.foxconn.plm.integrate.config.dataSource.DataSourceType;
import com.foxconn.plm.integrate.config.dataSource.DynamicDataSourceContextHolder;
import com.foxconn.plm.integrate.spas.domain.ReportPojo;
import com.foxconn.plm.integrate.spas.mapper.SpasMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.QueryUtil;
import com.foxconn.plm.utils.tc.StructureManagementUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class EE3DReportService {

    @Resource
    private CISMapper cisMapper;

    @Resource
    private SpasMapper spasMapper;

    private final String[] PART_TYPES = new String[]{"Connector\\", "Optoelectronics\\LED", "Electromechanical Device\\Switch", "Mechanical " +
            "Hardware\\Heat Sink"};

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
                List<ReportPojo> projects = spasMapper.queryProjectById(projectId);
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
                Folder folder = findChildFolderByName(ebomFolder, e.getProjectSeries(), ds);
                e.setTempFolder(folder);
            }).map(e -> getReportBeansByFolder(e, "projectName", ds)).flatMap(Collection::stream).map(e -> {
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
            Folder bomFolder = findChildFolderByName(e.getTempFolder(), e.getTempFolderName(), ds);
            if (bomFolder != null) {
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
                            beans.add(reportBean);
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            return beans;
        }).flatMap(Collection::parallelStream), sessionService, tcsoaServiceFactory));
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
        if (parentBean.getTempFolder() != null) {
            Set<Folder> folders = findChildFolders(parentBean.getTempFolder(), ds);
            if (folders != null) {
                ds.getProperties(folders.toArray(new ModelObject[0]), new String[]{"object_name"});
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
            }).map(e -> getReportBeansByFolder(e, "phase", ds)).flatMap(Collection::parallelStream)
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
                                    beanList.add(reportBean);
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
                    if (StringUtils.hasLength(partType)) {
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

    public List<EE3DReportBean> getItemsClsData(Set<ItemRevision> itemRevs, String customer, String bu, DataManagementService ds) throws
            NotLoadedException {
        List<EE3DReportBean> list = new ArrayList<>();
        ds.getProperties(itemRevs.toArray(new ModelObject[0]), new String[]{"d9_PartType", "item_id"});
        List<Item3DInfo> list3ds = null;
        if ("DT".equalsIgnoreCase(bu) && "dell".equalsIgnoreCase(customer)) {
            list3ds = getItem3DList_Dell(itemRevs, ds);
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

    class Item3DInfo {
        String partPn;
        String partType;
        String category;
        boolean has3D = false;
    }


    Set<Folder> findChildFolders(Folder parentFolder, DataManagementService dataManagementService) {
        try {
            Set<Folder> childFolders = new HashSet<>();
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
        return null;
    }

    Folder findChildFolderByName(Folder parentFolder, String folderName, DataManagementService ds) {
        try {
            //ds.refreshObjects(new ModelObject[]{parentFolder});
            getProperty(ds, parentFolder, "contents");
            WorkspaceObject[] contents = parentFolder.get_contents();
            for (WorkspaceObject content : contents) {
                if (content instanceof Folder) {
                    Folder folder = (Folder) content;
                    String folderNameTemp = getFolderName(folder, ds);
                    if (folderNameTemp.equals(folderName)) {
                        return folder;
                    }
                }
            }
            return null;
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

}
