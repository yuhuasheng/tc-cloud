package com.foxconn.plm.tcservice.benefitreport.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.MegerCellEntity;
import com.foxconn.plm.entity.response.PhaseBean;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.SPASProject;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.tcservice.benefitreport.constant.*;
import com.foxconn.plm.tcservice.benefitreport.domain.*;
import com.foxconn.plm.tcservice.benefitreport.service.SpasProjectService;
import com.foxconn.plm.tcservice.mapper.master.BenefitReportMapper;
import com.foxconn.plm.tcservice.benefitreport.util.PropertitesUtil;

import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.date.DateUtil;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.math.MathUtil;
import com.foxconn.plm.utils.string.StringUtil;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.foxconn.plm.tcservice.benefitreport.constant.BenefitFilePathConstant.*;

/**
 * @Author HuashengYu
 * @Date 2022/7/13 10:13
 * @Version 1.0
 */
@Service
public class SpasProjectServiceImpl implements SpasProjectService {
    private static Log log = LogFactory.get();
    @Resource
    private TcIntegrateClient tcIntegrate;

    @Resource
    private BenefitReportMapper benefitReportMapper;

    private String BUNAME = ""; // 事业部名称
    private String CUSTOMERCODE = ""; // 客户代号
    private String LEVELS = ""; // 难易度
    private String PHASE = ""; // 阶段
    private Boolean UPDATEFLAG = false; // 作为是否需要成功更新的标识
    private String STARTDATE = ""; // 开始时间
    private String PROJECTID = ""; // 项目ID

    private int _thresholdForCToB = 0;

    @Override
    public R getSpasProjectByDate(String startDate, String bu,Workbook wb, Map<String,Integer> sheetIndexMap) {
        try {
            log.info("******** getSpasProjectByDate开始执行 ********");
            UPDATEFLAG = false;
            PROJECTID = ""; //将项目ID清空
            STARTDATE = startDate;
            BUNAME = bu;
            Date start = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
            SimpleDateFormat sdf = new SimpleDateFormat("MM");
            String format = sdf.format(start);
            int month = Integer.parseInt(format);
            int year = Integer.parseInt(new SimpleDateFormat("yyyy").format(start));
            String firstDayOfMonth = DateUtil.getFirstDayOfMonth(month);
            firstDayOfMonth = year + firstDayOfMonth.substring(4);
            firstDayOfMonth = firstDayOfMonth.substring(0, 10);
            log.info(month + " 月第一天：" + firstDayOfMonth);
            String lastDayOfMonth = DateUtil.getLastDayOfMonth(month);
            lastDayOfMonth = year + lastDayOfMonth.substring(4);
            lastDayOfMonth = lastDayOfMonth.substring(0, 10);
            log.info(month + " 月的最后一天:" + lastDayOfMonth);

            List<SPASProject> list = tcIntegrate.getClosedProjectsByDate(firstDayOfMonth, lastDayOfMonth, bu);
            if (CollectUtil.isEmpty(list)) {
                return getTemplateFile();
//                return new AjaxResult(500, bu + "," + month + "月份不存在专案", new ArrayList<BenefitReportBean>());
            }
            updateProjectFlag(list, bu); // 更新专案字段信息
            Collections.sort(list);
            if (CollectUtil.isEmpty(list)) {
                return getTemplateFile();
//                return new AjaxResult(500, bu + "," + month + "月份不存在专案", new ArrayList<BenefitReportBean>());
            }

            // 操作 ActionLog
            SimpleDateFormat sdfForYear = new SimpleDateFormat("yyyy");
            String formatForYear = sdfForYear.format(start);
            year = Integer.parseInt(formatForYear);
            List<ActionLogBean> actionLogLst = new ArrayList<>();
            List<ActionLogBean> actionLog = benefitReportMapper.getActionLog(year + "-" + String.format("%02d", month), bu);
            actionLog =
                    actionLog.stream().filter(CollectUtil.distinctByKey(bean -> bean.getFunctionName() + bean.getProject() + bean.getBu() + bean.getProjLevel() + bean.getPhase())).collect(Collectors.toList()); // 按照功能名 + 专案ID + BU + 阶段去重
//            actionLogLst.addAll(actionLog);
            actionLog.forEach(bean -> {
                actionLogLst.addAll(benefitReportMapper.getActionLogByBUAndPhase(bean.getPhase(), bean.getBu(), bean.getFunctionName(),
                        bean.getProject()));
            });

            // 过滤数据(根据 效益點 客户 專案難易程度 Phase)
            filterActionLog(actionLogLst);
            if (CollectUtil.isEmpty(actionLogLst)) {
                return getTemplateFile();
//                return new AjaxResult(500, bu + "," + month + "月份" + ",查询TC效益点记录不存在!", new ArrayList<BenefitReportBean>());
            }
            actionLogLst.forEach(obj -> { // 更新客户编号
                updateCustomerCode(obj); // 更新客户编号
                updateLevel(obj, bu); // 更新难易程度为系统难易程度
            });

            if (CollectUtil.isEmpty(actionLogLst)) {
                return getTemplateFile();
//                return new AjaxResult(500, bu + "," + month + "月份" + ",查询TC效益点记录不存在!", new ArrayList<BenefitReportBean>());
            }

            list = filterSPASProjectList(list, actionLogLst); // 过滤掉SPAS不符合要求的清单

//            Map<String, List<? extends Object>> map = getInterSection(list, actionLogLst);
//            list = (List<SPASProject>) map.get("SPASProjectList");
//            List<ActionLogBean> newActionLogLst = (List<ActionLogBean>) map.get("actionLogList");


            if (CollUtil.isEmpty(list)) {
                return getTemplateFile();
            }
            List<SPASProjectBean> resultList = groupByData(list);
            if (CollectUtil.isEmpty(resultList)) {
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "对SPAS数据分组失败");
            }
            Gson gson = new Gson();
            log.info(gson.toJson(resultList));
            String result = JSONUtil.toJsonPrettyStr(resultList);
            System.out.println(result);
            log.info("******** 执行完成 ********");

            // 分组(根据 效益點 客户 專案難易程度 Phase)
            Map<String, Map<String, Map<String, Map<String, List<ActionLogBean>>>>> actionLogMap = groupActionLog(actionLogLst);
            if (CollectUtil.isEmpty(actionLogMap)) {
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "对TC效益点专案信息分组失败");
            }
            list = list.stream().filter(CollectUtil.distinctByKey(spasProject -> spasProject.getProjectId() + spasProject.getProjectName())).collect(Collectors.toList()); // 过滤掉重复的专案，用作输出专案清单

            return updateSheetValue(actionLogMap, resultList, list, true, null,wb,sheetIndexMap); // 更新sheet内容
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        }
    }

    @Override
    public R getSingleSpasProject(String projectId, String projectName, String bu) {
        try {
            log.info("******** getSingleSpasProject 开始执行 ********");
            UPDATEFLAG = false;
            STARTDATE = ""; // 将开始时间清空
            PROJECTID = projectId;
            BUNAME = bu;
            projectId = StringUtil.replaceBlank(projectId).replace("p", "").replace("P", "");
            SPASProject projectPhase = tcIntegrate.getProjectPhase(StringUtil.replaceBlank(projectId));
            List<SPASProject> list = new ArrayList<>();
            list.add(projectPhase);
            updateProjectFlag(list, bu); // 更新SPAS专案字段信息
            if (CollectUtil.isEmpty(list)) {
                return getTemplateFile();
//                return new AjaxResult(500, "当前专案不符合输出报表专案", new ArrayList<BenefitReportBean>());
            }
            SPASProjectBean bean = new SPASProjectBean();
            bean.setSpasProject(projectPhase);
            bean.setBu(projectPhase.getBu());
            List<SPASProjectBean> resultList = new ArrayList<>();
            resultList.add(bean);
            log.info("******** getSingleSpasProject 执行完成 ********");

            // 操作 ActionLog
            List<ActionLogBean> actionLogLst = benefitReportMapper.getActionLogForSingle("P" + projectId, bu);
            filterActionLog(actionLogLst);
            if (CollectUtil.isEmpty(actionLogLst)) {
                return getTemplateFile();
//                return new AjaxResult(500, bu + "," + "专案名为:" + projectName + ",查询TC效益点记录不存在!", new ArrayList<BenefitReportBean>());
            }
            actionLogLst.forEach(obj -> { // 更新客户编号
                updateCustomerCode(obj); // 更新客户编号
                updateLevel(obj, bu); // 更新难易程度为系统难易程度
            });

            // 分组(根据 效益點 專案難易程度 Phase)
            Map<String, Map<String, Map<String, Map<String, List<ActionLogBean>>>>> actionLogMap = groupActionLog(actionLogLst);
            if (CollectUtil.isEmpty(actionLogMap)) {
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "对TC效益点专案信息分组失败");
            }
            Map<String,Integer> sheetIndexMap = new HashMap<>();
            sheetIndexMap.put("DT FTE效益計算",0);
            sheetIndexMap.put("MNT FTE效益計算",1);
            sheetIndexMap.put("PRT FTE效益計算",2);
            sheetIndexMap.put("专案清单",3);
            return updateSheetValue(actionLogMap, resultList, null, false, projectName,null,sheetIndexMap); // 更新sheet内容
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        }
    }

    @Override
    public void wirteExcel(Workbook wb, String bu, String startDate) {

    }

    @Override
    public void wirteExcelFor2022(Workbook wb, String bu, String startDate) throws ParseException {

    }


    /**
     * 更新SPAS专案字段信息
     *
     * @param list
     */
    public void updateProjectFlag(List<SPASProject> list, String bu) {
        removeInvalidRecord(list, bu); // 移除BU不存在的记录
        if (CollectUtil.isNotEmpty(list)) {
            list.parallelStream().forEach(info -> {
                updateCustomerCode(info); // 更新客户编号
                updateLevel(info, bu); // 更新难易程度为系统难易程度
            });
        }
    }

    /**
     * 移除BU不存在的记录
     *
     * @param list
     */
    private void removeInvalidRecord(List<SPASProject> list, String bu) {
        list.removeIf(project -> project.getBu() == null || "N/A".equals(project.getBu()) | "".equals(project.getBu()) | StringUtil.isEmpty(project.getProjectId())); // 移除BU为NA, 项目ID为空的记录
        list.removeIf(project -> StringUtil.isEmpty(project.getLevels()) || project.getLevels().split(",").length < 2);
        list.removeIf(project -> !project.getLevels().split(",")[1].contains(ProjectDifficulty.E1.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.E2.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.E3.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.A0.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.A.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.B.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.C.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.D.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.E.name()) &
                !project.getLevels().split(",")[1].toUpperCase().contains(ProjectDifficulty.F.name()));  // 移除专案等级不是E1，E2，E3, A0, A, B, C, D, E, F
        if (StringUtil.isNotEmpty(bu)) {
            list.removeIf(project -> !bu.equals(project.getBu())); // 移除和当前记录不同的BU记录
        }
    }

    private void filterActionLog(List<ActionLogBean> actionLogLst) {
        actionLogLst.removeIf(actionLog -> actionLog.getFunctionName() == null || "N/A".equals(actionLog.getFunctionName()) || "".equals(actionLog.getFunctionName())); // 移除FunctionName为NA,为空的记录
        actionLogLst.removeIf(actionLog -> actionLog.getCustom() == null || "N/A".equals(actionLog.getCustom()) || "".equals(actionLog.getCustom())); // 移除Custom为NA,为空的记录
        actionLogLst.removeIf(actionLog -> actionLog.getProjLevel() == null || "N/A".equals(actionLog.getProjLevel()) || "".equals(actionLog.getProjLevel())); // 移除ProjLevel为NA,为空的记录
        actionLogLst.removeIf(actionLog -> actionLog.getPhase() == null || "N/A".equals(actionLog.getPhase()) || "".equals(actionLog.getPhase()));
        // 移除Phase为NA,为空的记录
        actionLogLst.removeIf(actionLog -> !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.E1.name()) &
                !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.E2.name()) &
                !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.E3.name()) &
                !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.A0.name()) &
                !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.A.name()) &
                !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.B.name()) &
                !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.C.name()) &
                !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.D.name()) &
                !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.E.name()) &
                !actionLog.getProjLevel().toUpperCase().contains(ProjectDifficulty.F.name())); // 移除专案等级不是E1，E2，E3, A0, A, B, C, D, E, F
    }


    /**
     * 更新客户编号
     *
     * @param obj
     */
    private void updateCustomerCode(Object obj) {
        SPASProject info = null;
        ActionLogBean bean = null;
        String bu = null;
        String customer = null;
        if (obj instanceof SPASProject) {
            info = (SPASProject) obj;
            bu = info.getBu().toUpperCase().trim();
            if (BU.DT.name().equals(bu)) {
                info.setBu(BU.DT.name());
                customer = info.getCustomer().toUpperCase();
                if (customer.contains(Customer.Dell.customerName())) {
                    info.setCustomer(Customer.Dell.customerCode());
                } else if (customer.contains(Customer.HP.customerName())) {
                    info.setCustomer(Customer.HP.customerCode());
                } else if (customer.contains(Customer.LENOVO.customerName())) {
                    info.setCustomer(Customer.LENOVO.customerCode());
                }
            } else if (BU.MNT.name().equals(bu)) {
                info.setBu(BU.MNT.name());
                info.setCustomer(Customer.ALL.customerCode());
            } else if (BU.PRT.name().equals(bu)) {
                info.setBu(BU.PRT.name());
                info.setCustomer(Customer.PRINTER.customerCode());
            }
        } else if (obj instanceof ActionLogBean) {
            bean = (ActionLogBean) obj;
            bu = bean.getBu().toUpperCase().trim();
            if (BU.DT.name().equals(bu)) {
                bean.setBu(BU.DT.name());
                customer = bean.getCustom().toUpperCase();
                if (customer.contains(Customer.Dell.customerName())) {
                    bean.setCustom(Customer.Dell.customerCode());
                } else if (customer.contains(Customer.HP.customerName())) {
                    bean.setCustom(Customer.HP.customerCode());
                } else if (customer.contains(Customer.LENOVO.customerName())) {
                    bean.setCustom(Customer.LENOVO.customerCode());
                }
            } else if (BU.MNT.name().equals(bu)) {
                bean.setBu(BU.MNT.name());
                bean.setCustom(Customer.ALL.customerCode());
            } else if (BU.PRT.name().equals(bu)) {
                bean.setBu(BU.PRT.name());
                bean.setCustom(Customer.PRINTER.customerCode());
            }
        }
    }

    /**
     * 更新难易程度为系统难易程度
     *
     * @param obj
     */
    private void updateLevel(Object obj, String bu) {
        SPASProject info = null;
        ActionLogBean bean = null;
        String levels = null;
        if (obj instanceof SPASProject) {
            info = (SPASProject) obj;
            levels = info.getLevels();
            String[] split = levels.split(",");
            String str = split[1].toUpperCase();
            if (!BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.E1.name())) {
                info.setLevels(ProjectDifficulty.E1.name());
            } else if (!BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.E2.name())) {
                info.setLevels(ProjectDifficulty.E2.name());
            } else if (!BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.E3.name())) {
                info.setLevels(ProjectDifficulty.E3.name());
            } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.A0.name())) {
                info.setLevels(ProjectDifficulty.A0.name());
            } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.A.name())) {
                info.setLevels(ProjectDifficulty.A.name());
            } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.B.name())) {
                info.setLevels(ProjectDifficulty.B.name());
            } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.C.name())) {
                info.setLevels(ProjectDifficulty.C.name());
            } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.D.name())) {
                info.setLevels(ProjectDifficulty.D.name());
            } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.E.name())) {
                info.setLevels(ProjectDifficulty.E.name());
            } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.F.name())) {
                info.setLevels(ProjectDifficulty.F.name());
            }
        } else if (obj instanceof ActionLogBean) {
            bean = (ActionLogBean) obj;
            levels = bean.getProjLevel();
//            String[] split = levels.split(",");
//            String str = split[1].toUpperCase();
            if (BU.MNT.name().equals(bu) && levels.contains(ProjectDifficulty.A0.name())) {
                bean.setProjLevel(ProjectDifficulty.A0.name());
            } else if (BU.MNT.name().equals(bu) && levels.contains(ProjectDifficulty.A.name())) {
                bean.setProjLevel(ProjectDifficulty.A.name());
            } else if (BU.MNT.name().equals(bu) && levels.contains(ProjectDifficulty.B.name())) {
                bean.setProjLevel(ProjectDifficulty.B.name());
            } else if (BU.MNT.name().equals(bu) && levels.contains(ProjectDifficulty.C.name())) {
                bean.setProjLevel(ProjectDifficulty.C.name());
            } else if (BU.MNT.name().equals(bu) && levels.contains(ProjectDifficulty.D.name())) {
                bean.setProjLevel(ProjectDifficulty.D.name());
            } else if (BU.MNT.name().equals(bu) && levels.contains(ProjectDifficulty.E.name())) {
                bean.setProjLevel(ProjectDifficulty.E.name());
            } else if (BU.MNT.name().equals(bu) && levels.contains(ProjectDifficulty.F.name())) {
                bean.setProjLevel(ProjectDifficulty.F.name());
            }
        }

    }

    private List<SPASProjectBean> groupByData(List<SPASProject> list) {
        log.info("==>> 开始执行分组: ");
        List<SPASProjectBean> resultList = new ArrayList<>();
        Map<String, List<SPASProject>> buGroup = list.stream().collect(Collectors.groupingBy(bean -> bean.getBu()));
        buGroup.forEach((key, value) -> {
            SPASProjectBean rootBean = new SPASProjectBean();
            rootBean.setBu(key);
            List<SPASProject> valueList = value;
//            rootBean.setList(valueList);
            Map<String, List<SPASProject>> levelGroup = valueList.stream().collect(Collectors.groupingBy(bean1 -> bean1.getLevels()));
            levelGroup.forEach((key1, value1) -> {
                SPASProjectBean bean1 = new SPASProjectBean();
                List<SPASProject> valueList1 = value1;
                bean1.setLevels(key1);
//                bean1.setList(valueList1);
                rootBean.addChild(bean1);
                Map<String, List<SPASProject>> phaseGroup = valueList1.stream().collect(Collectors.groupingBy(bean2 -> bean2.getPhase()));
                phaseGroup.forEach((key2, value2) -> {
                    SPASProjectBean bean2 = new SPASProjectBean();
                    List<SPASProject> valueList2 = value2;
                    bean2.setPhase(key2);
//                    bean2.setList(valueList2);
                    bean1.addChild(bean2);
                    Map<String, List<SPASProject>> customerGroup = valueList2.stream().collect(Collectors.groupingBy(bean3 -> bean3.getCustomer()));
                    customerGroup.forEach((key3, value3) -> {
                        SPASProjectBean bean3 = new SPASProjectBean();
                        List<SPASProject> valueList3 = value3;
                        bean3.setCustomer(key3);
                        bean3.setList(valueList3);
                        bean2.addChild(bean3);
                    });
                });
            });
            resultList.add(rootBean);
        });
        log.info("==>> 分组完成...");
        return resultList;
    }


    /**
     * 更新sheet内容
     *
     * @param resultList
     * @param flag
     * @return
     * @throws IOException
     */
    private R updateSheetValue(Map<String, Map<String, Map<String, Map<String, List<ActionLogBean>>>>> actionLogMap,
                               List<SPASProjectBean> resultList, List<SPASProject> list, Boolean flag, String projectName,Workbook wb,Map<String,Integer> sheetIndexMap) throws IOException {
        boolean externWorkbook = (wb != null);
        FileInputStream fis = null;
        File destFile = null;
        String destFilePath = null;
        if (MathUtil.base64De(DESTDIR).endsWith(File.separator)) {
            destFilePath = MathUtil.base64De(DESTDIR) + UUID.randomUUID().toString().replace("-", "") + "_" + MathUtil.base64De(REPORTFILESUFFIX);
        } else {
            destFilePath =
                    MathUtil.base64De(DESTDIR) + File.separator + UUID.randomUUID().toString().replace("-", "") + "_" + MathUtil.base64De(REPORTFILESUFFIX);
        }
        try {
            File dir = new File(MathUtil.base64De(DESTDIR));
            if (!dir.exists()) {
                dir.mkdirs();
            }
            destFile = new File(destFilePath);
            if (destFile.exists()) {
                destFile.delete();
            }

            if (wb == null) {
                wb = ExcelUtil.getWorkbookNew(MathUtil.base64De(TEMPLATEPATH));
            }
            wb.setForceFormulaRecalculation(true);

            Sheet sheet = null;
            List<String> benefitPointList = null;
            for (SPASProjectBean rootBean : resultList) {
                BUNAME = rootBean.getBu();
                if (BU.DT.name().equals(rootBean.getBu())) {
                    _thresholdForCToB = 27;
                    sheet = wb.getSheetAt(sheetIndexMap.get("DT FTE效益計算"));
                    benefitPointList = new ArrayList<String>(Arrays.asList(DTBenefitPoint.DTStringArray));
                } else if (BU.MNT.name().equals(rootBean.getBu())) {
                    _thresholdForCToB = 9;
                    sheet = wb.getSheetAt(sheetIndexMap.get("MNT FTE效益計算"));
                    benefitPointList = new ArrayList<String>(Arrays.asList(MNTBenefitPoint.MNTStringArray));
                } else if (BU.PRT.name().equals(rootBean.getBu())) {
                    _thresholdForCToB = 18;
                    sheet = wb.getSheetAt(sheetIndexMap.get("PRT FTE效益計算"));
                    benefitPointList = new ArrayList<String>(Arrays.asList(PRTBenefitPoint.DTStringArray));
                }
                if (flag) {
                    updateAllProjectCellValue(sheet, rootBean, benefitPointList); // 更新单元格内容(所有专案)
                } else {
                    updateSingleProjectCellValue(sheet, rootBean, benefitPointList); // 更新单元格内容(单个专案)
                }
            }

            // 更新 b ，c
            updateCellValueForBForC(sheet, actionLogMap);
            if (UPDATEFLAG) {
                generateProjectList(list, wb,sheetIndexMap); // 更新单元格内容成功，再生成专案列表
            } else {
                if (StringUtil.isNotEmpty(projectName)) {
                    return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "当前专案: " + projectName + ", 不符合当前统计的效益报表的要求");
                }
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "当前时间段不存在符合的专案");
            }
            return R.success("success", destFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            log.error(destFilePath + ", 更新失败");
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "更新效益表数据失败");
        } finally {
            if (!externWorkbook && wb != null) {
                wb.setForceFormulaRecalculation(true);
                if (destFile != null) {
                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(destFile);
                        wb.write(out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        wb.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (out != null) {
//                        out.flush();
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 返回模板文件
     *
     * @return
     * @throws IOException
     */
    private R getTemplateFile() throws IOException {
        Workbook wb = null;
        File destFile = null;
        String destFilePath = null;
        if (MathUtil.base64De(DESTDIR).endsWith(File.separator)) {
            destFilePath = MathUtil.base64De(DESTDIR) + UUID.randomUUID().toString().replace("-", "") + "_" + MathUtil.base64De(REPORTFILESUFFIX);
        } else {
            destFilePath =
                    MathUtil.base64De(DESTDIR) + File.separator + UUID.randomUUID().toString().replace("-", "") + "_" + MathUtil.base64De(REPORTFILESUFFIX);
        }
        try {
            File dir = new File(MathUtil.base64De(DESTDIR));
            if (!dir.exists()) {
                dir.mkdirs();
            }
            destFile = new File(destFilePath);
            if (destFile.exists()) {
                destFile.delete();
            }

            System.out.println(MathUtil.base64De(TEMPLATEPATH));
            wb = ExcelUtil.getWorkbookNew(MathUtil.base64De(TEMPLATEPATH));
            wb.setForceFormulaRecalculation(true);

            return R.success("success", destFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (wb != null) {
                wb.setForceFormulaRecalculation(true);
                if (destFile != null) {
                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(destFile);
                        wb.write(out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        wb.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (out != null) {
//                        out.flush();
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }
        return null;
    }


    /**
     * 生成专案列表
     *
     * @param list
     * @param wb
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws ClassNotFoundException
     */
    private void generateProjectList(List<SPASProject> list, Workbook wb,Map<String,Integer> sheetIndexMap) throws IllegalAccessException, NoSuchFieldException,
            ClassNotFoundException {
        if (CollectUtil.isNotEmpty(list)) {
            Sheet sheet =wb.getSheetAt(sheetIndexMap.get("专案清单"));
            CellStyle cellStyle = ExcelUtil.getCellStyle(wb);
            ExcelUtil.setCellValue(list, ProjectListSheetConstant.start, ProjectListSheetConstant.COLLENGTH, sheet, cellStyle); // 输出专案清单

            List<MegerCellEntity> buList = ExcelUtil.scanMegerCells(list, "bu", 0);
            buList.forEach(buMegerCellEntity -> {
                sheet.addMergedRegion(new CellRangeAddress(buMegerCellEntity.startRow, buMegerCellEntity.endRow, 0, 0));
            });

            List<MegerCellEntity> levelsList = ExcelUtil.scanMegerCells(list, "levels", 0);
            levelsList.forEach(levelsMegerCellEntity -> {
                sheet.addMergedRegion(new CellRangeAddress(levelsMegerCellEntity.startRow, levelsMegerCellEntity.endRow, 1, 1));
            });

            List<MegerCellEntity> phaseList = ExcelUtil.scanMegerCells(list, "phase", 0);
            phaseList.forEach(phaseMegerCellEntity -> {
                sheet.addMergedRegion(new CellRangeAddress(phaseMegerCellEntity.startRow, phaseMegerCellEntity.endRow, 2, 2));
            });

            List<MegerCellEntity> customerList = ExcelUtil.scanMegerCells(list, "customer", 0);
            customerList.forEach(customerMegerCellEntity -> {
                sheet.addMergedRegion(new CellRangeAddress(customerMegerCellEntity.startRow, customerMegerCellEntity.endRow, 3, 3));
            });

        }
    }


    /**
     * 更新单元格内容(所有专案)
     *
     * @param sheet
     * @param rootBean
     * @throws IOException
     */
    private void updateAllProjectCellValue(Sheet sheet, SPASProjectBean rootBean, List<String> benefitPointList) throws IOException {
        List<SPASProjectBean> childs = rootBean.getChilds();
        if (CollectUtil.isEmpty(childs)) {
            CUSTOMERCODE = rootBean.getCustomer();
            int number = 0;
            List<SPASProject> spasProjectList = rootBean.getList();
            if (CollectUtil.isNotEmpty(spasProjectList)) {
                spasProjectList =
                        spasProjectList.stream().filter(CollectUtil.distinctByKey(spasProject -> spasProject.getProjectId() + spasProject.getProjectName())).collect(Collectors.toList());
                number = spasProjectList.size();
            }
//            int number = rootBean.getList().size();
            String key = BUNAME + "," + CUSTOMERCODE + "," + LEVELS + "," + PHASE;
            log.info("==>> 月份效益key: " + key);
            String index = PropertitesUtil.props.getProperty(key);
            if (StringUtil.isEmpty(index)) {
                log.info("==>> 月份效益index 没有匹配到");
                return;
            }
            log.info("==>> 月份效益index: " + index);
            ExcelUtil.setValueAtForDouble(sheet, Integer.parseInt(Optional.ofNullable(index.split(",")[0]).orElse("0")) - 1,
                    Integer.parseInt(Optional.ofNullable(index.split(",")[1]).orElse("0")) - 1, number);
            UPDATEFLAG = true;
        }

        if (StringUtil.isNotEmpty(rootBean.getLevels())) {
            LEVELS = rootBean.getLevels();
        }
        if (StringUtil.isNotEmpty(rootBean.getPhase())) {
            PHASE = rootBean.getPhase();
        }
        for (SPASProjectBean bean : childs) {
            updateAllProjectCellValue(sheet, bean, benefitPointList);
        }
    }

    /**
     * 更新单元格内容(单个专案)
     *
     * @param sheet
     * @param bean
     * @param benefitPointList
     */
    private void updateSingleProjectCellValue(Sheet sheet, SPASProjectBean bean, List<String> benefitPointList) {
        SPASProject spasProject = bean.getSpasProject();
        if (spasProject == null) {
            return;
        }
        LEVELS = spasProject.getLevels();
        CUSTOMERCODE = spasProject.getCustomer();
        List<PhaseBean> phases = spasProject.getPhases();
        if (CollectUtil.isNotEmpty(phases)) {
            phases.forEach(str -> {
                PHASE = str.getName();
                String key = BUNAME + "," + CUSTOMERCODE + "," + LEVELS + "," + PHASE;
                log.info("==>> 专案效益key: " + key);
                String index = PropertitesUtil.props.getProperty(key);
                if (StringUtil.isEmpty(index)) {
                    log.info("==>> 专案效益index 没有匹配到");
                    return;
                }

                try {
                    log.info("==>> 专案效益index: " + index);
                    ExcelUtil.setValueAtForDouble(sheet, Integer.parseInt(Optional.ofNullable(index.split(",")[0]).orElse("0")) - 1,
                            Integer.parseInt(Optional.ofNullable(index.split(",")[1]).orElse("0")) - 1, 1);
//                    updateBenefitPointValue(sheet, excelUtils, benefitPointList); // 更新效益点c值内容
                    UPDATEFLAG = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    log.error(e.getLocalizedMessage());
                    throw new RuntimeException();
                }
            });
        }
    }


    /**
     * 尝试从文本中获取效益点的C列值
     *
     * @param list
     * @param key
     * @return
     */
    private String getBenefitPointValue(List<String> list, String key) {
        if (CollectUtil.isEmpty(list)) {
            return "";
        }
        Optional<String> findAny = list.stream().filter(str -> {
            return str.split("=")[0].equals(key);
        }).findAny();
        if (findAny.isPresent()) {
            return findAny.get().split("=")[1];
        }
        return "";
    }

    private Map<String, Map<String, Map<String, Map<String, List<ActionLogBean>>>>> groupActionLog(List<ActionLogBean> actionLogLst) {
        Map<String, Map<String, Map<String, Map<String, List<ActionLogBean>>>>> retMap = new HashMap<>();

        Map<String, List<ActionLogBean>> functionNameGroup = actionLogLst.stream().collect(Collectors.groupingBy(bean -> bean.getFunctionName()));
        functionNameGroup.forEach((k1, v1) -> {
            Map<String, Map<String, Map<String, List<ActionLogBean>>>> customMap = new HashMap<>();

            Map<String, List<ActionLogBean>> customGroup = v1.stream().collect(Collectors.groupingBy(bean -> bean.getCustom()));
            customGroup.forEach((k2, v2) -> {
                Map<String, Map<String, List<ActionLogBean>>> projLevelMap = new HashMap<>();

                Map<String, List<ActionLogBean>> projLevelGroup = v2.stream().collect(Collectors.groupingBy(bean -> bean.getProjLevel()));
                projLevelGroup.forEach((k3, v3) -> {
                    Map<String, List<ActionLogBean>> phaseGroup = v3.stream().collect(Collectors.groupingBy(bean -> bean.getPhase()));
                    projLevelMap.put(k3, phaseGroup);
                });

                customMap.put(k2, projLevelMap);
            });

            retMap.put(k1, customMap);
        });

        return retMap;
    }

    private void updateCellValueForBForC(Sheet sheet, Map<String, Map<String, Map<String, Map<String, List<ActionLogBean>>>>> actionLogMap) {
        actionLogMap.forEach((functionNameKey, functionNameValue) -> {
            functionNameValue.forEach((customKey, customValue) -> {
                customValue.forEach((projLevelKey, projLevelValue) -> {
                    projLevelValue.forEach((phaseKey, phaseValue) -> {
                        try {
                            String key = BUNAME + "," + customKey + "," + functionNameKey + "," + projLevelKey + "," + phaseKey;
                            log.info("==>> 月份效益key: " + key);
                            String index = PropertitesUtil.props.getProperty(key);
                            if (StringUtil.isEmpty(index)) {
                                log.info("==>> 月份效益index 没有匹配到");
                                return;
                            }
                            log.info("==>> 月份效益index: " + index);

                            int rowIndex = Integer.parseInt(Optional.ofNullable(index.split(",")[0]).orElse("0")) - 1;
                            int colIndex = -1;

                            // b value
                            colIndex = Integer.parseInt(Optional.ofNullable(index.split(",")[1]).orElse("0")) - 1 - _thresholdForCToB;
                            ExcelUtil.setValueAtForDouble(sheet, rowIndex, colIndex, getTimeCount(phaseValue));
                            // c value
                            colIndex = Integer.parseInt(Optional.ofNullable(index.split(",")[1]).orElse("0")) - 1;
                            phaseValue =
                                    phaseValue.stream().filter(CollectUtil.distinctByKey(bean -> bean.getFunctionName() + bean.getProject() + bean.getProjLevel() + bean.getPhase())).collect(Collectors.toList());
                            ExcelUtil.setValueAtForDouble(sheet, rowIndex, colIndex, phaseValue.size());
                            UPDATEFLAG = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                });
            });

        });
    }

    private double getTimeCount(List<ActionLogBean> actionLogLst) {
        double retVal = 0.;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

            for (ActionLogBean actionLog : actionLogLst) {
                long startTimeLng = DateUtil.stringTime2LongTime(actionLog.getStartTime(), sdf.toPattern());
                long endTimeLng = DateUtil.stringTime2LongTime(actionLog.getEndTime(), sdf.toPattern());

                retVal += (endTimeLng - startTimeLng);
            }

            if (retVal > 0) {
//                retVal = retVal / 1000;
//                retVal = Double.valueOf(CommonTools.formatDecimal(String.valueOf(retVal / (1000 * 3600)), 2));
                retVal = Double.valueOf(String.valueOf(retVal / (1000 * 3600)));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retVal;
    }

    /**
     * 过滤掉SPAS不符合要求的清单
     *
     * @param list
     * @param actionLogLst
     * @return
     */
    private List<SPASProject> filterSPASProjectList(List<SPASProject> list, List<ActionLogBean> actionLogLst) {
        List<SPASProject> spasProjectList = new ArrayList<>();
        list.forEach(spasProject -> {
            boolean flag = actionLogLst.stream().anyMatch(bean -> ("p" + spasProject.getProjectId()).equalsIgnoreCase(bean.getProject())); //
            // 检查是否至少匹配一个元素
            if (flag) {
                spasProjectList.add(spasProject);
            }
        });
        return spasProjectList;
    }


    /**
     * 获取SPAS和Action_Log的相同项
     *
     * @param list
     * @param actionLogLst
     * @return
     */
    private Map<String, List<? extends Object>> getInterSection(List<SPASProject> list, List<ActionLogBean> actionLogLst) {
        Map<String, List<? extends Object>> retMap = new LinkedHashMap<>();
        List<SPASProject> newSPASList = new ArrayList<>();
        List<ActionLogBean> newActionLogList = new ArrayList<>();
        List<String> interSectProjectIds = new CopyOnWriteArrayList<>();

        for (SPASProject spasProject : list) {
            String spasProjectId = "P" + spasProject.getProjectId();
            for (ActionLogBean bean : actionLogLst) {
                String projectId = bean.getProject();
                if (spasProjectId.equalsIgnoreCase(projectId)) {
                    interSectProjectIds.add(spasProjectId);
                }
            }
        }

        if (CollUtil.isEmpty(interSectProjectIds)) {
            return null;
        }

        interSectProjectIds = interSectProjectIds.stream().filter(CollectUtil.distinctByKey(str -> str)).collect(Collectors.toList());
        for (SPASProject spasProject : list) {
            boolean flag = interSectProjectIds.stream().anyMatch(projectId -> projectId.equalsIgnoreCase("P" + spasProject.getProjectId()));
            if (flag) {
                newSPASList.add(spasProject);
            }
        }

        for (ActionLogBean bean : actionLogLst) {
            boolean flag = interSectProjectIds.stream().anyMatch(projectId -> projectId.equalsIgnoreCase(bean.getProject()));
            if (flag) {
                newActionLogList.add(bean);
            }
        }

        retMap.put("SPASProjectList", newSPASList);
        retMap.put("actionLogList", newActionLogList);
        return retMap;
    }

    public static void main(String[] args) {
        System.out.println(MathUtil.base64De(TEMPLATEPATH));
    }
}
