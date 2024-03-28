package com.foxconn.plm.tcservice.projectReport;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.response.MegerCellEntity;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.projectReport.dto.rv.*;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.file.FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Api(tags = "TC专案执行报表看板开发")
@RestController
@RequestMapping("/projectPerformance")
public class ProjectPerformanceController {
    private static Log log = LogFactory.get();
    static Map<String, Map<String, Integer>> sortRole = new HashMap<>();

    static {
        String[] array = new String[]{"EE", "DC", "SD", "TA", "PSU", "ME", "Layout", "SIM", "ID", "CE", "EIV", "EV", "Env", "SPM", "SIT", "FWOD OBD/GFX", "PCA PM", "RF", "Safety", "PAC", "FW-BIOS", "Speaker", "SW-L6 Diag", "EMC"};
        Map<String, Integer> dtSort = new HashMap<>();
        for (int i = 0; i < array.length; i++) {
            dtSort.put(array[i].trim().toUpperCase(Locale.ROOT), i);
        }
        sortRole.put("DT", dtSort);

        array = new String[]{"BOM", "CE", "DQA", "EE", "EMC", "FW", "ID", "Layout", "LCM", "ME", "PA", "Panel CE", "PM", "PSU", "Safety"};
        Map<String, Integer> prtSort = new HashMap<>();
        for (int i = 0; i < array.length; i++) {
            prtSort.put(array[i].trim().toUpperCase(Locale.ROOT), i);
        }
        sortRole.put("MNT", prtSort);

        array = new String[]{"CE", "EE", "ME", "Layout", "PDE", "PM"};
        Map<String, Integer> mntSort = new HashMap<>();
        for (int i = 0; i < array.length; i++) {
            mntSort.put(array[i].trim().toUpperCase(Locale.ROOT), i);
        }
        sortRole.put("PRT", mntSort);
    }

    @Resource
    ProjectReportService projectReportService;

    @ApiOperation("查询报表")
    @PostMapping("/search")
    public R<ProjectPerformanceRv> report() throws CloneNotSupportedException {

        ProjectPerformanceRv rv = new ProjectPerformanceRv();
        ArrayList<BUProjectOnlineDetail> buList = new ArrayList<>();
        ArrayList<CustomerProjectOnlineDetail> customerList = new ArrayList<>();
        ArrayList<CumulativeOutput> cumulativeOutputList = new ArrayList<>();
        ArrayList<ProductProjectOutput> productProjectOutputList = new ArrayList<>();
        List<FunctionOnlineDetail> functionOnlineDetailList = new ArrayList<>();

        rv.setBuList(buList);
        rv.setCustomerList(customerList);
        rv.setCumulativeOutputList(cumulativeOutputList);
        rv.setProductProjectOutputList(productProjectOutputList);
        rv.setFunctionOnlineDetailList(functionOnlineDetailList);

        List<ReportEntity> list = new ArrayList<>();
        List<ReportEntity> projectDeptList = new ArrayList<>();
        QueryEntity query = new QueryEntity();
        List<ReportEntity> sourceList = projectReportService.queryData(query);
        sourceList.removeIf(entity -> entity.shouldOutputDeliverableQty == 0);
        ReportEntity lastPidReportEntity = new ReportEntity();
        for (ReportEntity reportEntity : sourceList) {
            String pid = reportEntity.pid;

            if (pid.equals(lastPidReportEntity.pid)) {
                lastPidReportEntity.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                lastPidReportEntity.outputDeliverableQty += reportEntity.outputDeliverableQty;
                lastPidReportEntity.archivedQty += reportEntity.archivedQty;
                lastPidReportEntity.shouldOutputDeliverableQty += reportEntity.shouldOutputDeliverableQty;
            } else {
                lastPidReportEntity = reportEntity.clone();
                list.add(lastPidReportEntity);
            }
        }
        for (ReportEntity reportEntity : sourceList) {
            String pid = reportEntity.pid;
            String dept = reportEntity.dept;
            ReportEntity projectDept = findProjectDept(projectDeptList, pid, dept);
            if (projectDept == null) {
                projectDeptList.add(reportEntity.clone());
            } else {
                projectDept.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                projectDept.outputDeliverableQty += reportEntity.outputDeliverableQty;
            }
        }


        BUProjectOnlineDetail prt = new BUProjectOnlineDetail("PRT");
        BUProjectOnlineDetail mnt = new BUProjectOnlineDetail("MNT");
        BUProjectOnlineDetail dt = new BUProjectOnlineDetail("DT");
        CustomerProjectOnlineDetail lenovo = new CustomerProjectOnlineDetail("Lenovo");
        CustomerProjectOnlineDetail dell = new CustomerProjectOnlineDetail("Dell");
        CustomerProjectOnlineDetail hp = new CustomerProjectOnlineDetail("HP");
        CumulativeOutput cumulativeOutputDtDell = new CumulativeOutput("DT-Dell");
        CumulativeOutput cumulativeOutputDtHp = new CumulativeOutput("DT-HP");
        CumulativeOutput cumulativeOutputDtLenovo = new CumulativeOutput("DT-Lenovo");
        CumulativeOutput cumulativeOutputMNT = new CumulativeOutput("MNT");
        CumulativeOutput cumulativeOutputPRT = new CumulativeOutput("PRT");
        ProductProjectOutput productProjectOutputDtDell = new ProductProjectOutput("DT-Dell");
        ProductProjectOutput productProjectOutputDtHP = new ProductProjectOutput("DT-HP");
        ProductProjectOutput productProjectOutputDtLenovo = new ProductProjectOutput("DT-Lenovo");
        ProductProjectOutput productProjectOutputMNT = new ProductProjectOutput("MNT");
        ProductProjectOutput productProjectOutputPRT = new ProductProjectOutput("PRT");

        for (ReportEntity reportEntity : list) {
            String bu = reportEntity.bu;
            String customer = reportEntity.customer;
            int qty = reportEntity.workflowDiagramDocumentQty + reportEntity.outputDeliverableQty;
            if ("PRT".equals(bu)) {
                if (qty > 0) {
                    prt.online++;
                } else {
                    prt.notOnline++;
                }
                prt.total++;
                cumulativeOutputPRT.shouldOutQty += reportEntity.shouldOutputDeliverableQty;
                cumulativeOutputPRT.outQty += reportEntity.outputDeliverableQty;
                cumulativeOutputPRT.setRate(Float.parseFloat(String.format("%.2f", cumulativeOutputPRT.outQty * 1.0f / (cumulativeOutputPRT.shouldOutQty == 0 ? 1 : cumulativeOutputPRT.shouldOutQty) * 100)));
                productProjectOutputPRT.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                productProjectOutputPRT.archivedQty += reportEntity.archivedQty;
            }
            if ("MNT".equals(bu)) {
                if (qty > 0) {
                    mnt.online++;
                } else {
                    mnt.notOnline++;
                }
                mnt.total++;
                cumulativeOutputMNT.shouldOutQty += reportEntity.shouldOutputDeliverableQty;
                cumulativeOutputMNT.outQty += reportEntity.outputDeliverableQty;
                cumulativeOutputMNT.setRate(Float.parseFloat(String.format("%.2f", cumulativeOutputMNT.outQty * 1.0f / (cumulativeOutputMNT.shouldOutQty == 0 ? 1 : cumulativeOutputMNT.shouldOutQty) * 100)));
                productProjectOutputMNT.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                productProjectOutputMNT.archivedQty += reportEntity.archivedQty;
            }
            if ("DT".equals(bu)) {
                if (qty > 0) {
                    dt.online++;
                } else {
                    dt.notOnline++;
                }
                dt.total++;
                if (customer.startsWith("Lenovo")) {
                    if (qty > 0) {
                        lenovo.online++;
                    } else {
                        lenovo.notOnline++;
                    }
                    lenovo.total++;
                    cumulativeOutputDtLenovo.shouldOutQty += reportEntity.shouldOutputDeliverableQty;
                    cumulativeOutputDtLenovo.outQty += reportEntity.outputDeliverableQty;
                    cumulativeOutputDtLenovo.setRate(Float.parseFloat(String.format("%.2f", cumulativeOutputDtLenovo.outQty * 1.0f / (cumulativeOutputDtLenovo.shouldOutQty == 0 ? 1 : cumulativeOutputDtLenovo.shouldOutQty) * 100)));
                    productProjectOutputDtLenovo.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                    productProjectOutputDtLenovo.archivedQty += reportEntity.archivedQty;
                }
                if (customer.equals("Dell")) {
                    if (qty > 0) {
                        dell.online++;
                    } else {
                        dell.notOnline++;
                    }
                    dell.total++;
                    cumulativeOutputDtDell.shouldOutQty += reportEntity.shouldOutputDeliverableQty;
                    cumulativeOutputDtDell.outQty += reportEntity.outputDeliverableQty;
                    cumulativeOutputDtDell.setRate(Float.parseFloat(String.format("%.2f", cumulativeOutputDtDell.outQty * 1.0f / (cumulativeOutputDtDell.shouldOutQty == 0 ? 1 : cumulativeOutputDtDell.shouldOutQty) * 100)));
                    productProjectOutputDtDell.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                    productProjectOutputDtDell.archivedQty += reportEntity.archivedQty;
                }
                if (customer.equals("HP")) {
                    if (qty > 0) {
                        hp.online++;
                    } else {
                        hp.notOnline++;
                    }
                    hp.total++;
                    cumulativeOutputDtHp.shouldOutQty += reportEntity.shouldOutputDeliverableQty;
                    cumulativeOutputDtHp.outQty += reportEntity.outputDeliverableQty;
                    cumulativeOutputDtHp.setRate(Float.parseFloat(String.format("%.2f", cumulativeOutputDtHp.outQty * 1.0f / (cumulativeOutputDtHp.shouldOutQty == 0 ? 1 : cumulativeOutputDtHp.shouldOutQty) * 100)));
                    productProjectOutputDtHP.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                    productProjectOutputDtHP.archivedQty += reportEntity.archivedQty;
                }
            }
        }

        for (ReportEntity reportEntity : projectDeptList) {
            String bu = reportEntity.bu;
            String dept = reportEntity.dept;
            if (!"DT".equals(bu)) {
                continue;
            }
            int qty = reportEntity.workflowDiagramDocumentQty + reportEntity.outputDeliverableQty;
            FunctionOnlineDetail functionOnlineDetail = findFunctionOnlineDetail(functionOnlineDetailList, dept);
            if (functionOnlineDetail == null) {
                functionOnlineDetail = new FunctionOnlineDetail(dept);
                functionOnlineDetailList.add(functionOnlineDetail);
            }
            if (qty > 0) {
                functionOnlineDetail.online++;
            }
            functionOnlineDetail.totalQty++;
            functionOnlineDetail.setRate(Float.parseFloat(String.format("%.2f", functionOnlineDetail.online * 1.0f / (functionOnlineDetail.totalQty == 0 ? 1 : functionOnlineDetail.totalQty) * 100)));
        }

        functionOnlineDetailList.sort((o1, o2) -> {
            Map<String, Integer> sort = sortRole.get("DT");
            Integer i = sort.get(o1.getName().trim().toUpperCase(Locale.ROOT));
            if (i == null) {
                i = Integer.MAX_VALUE;
            }
            Integer j = sort.get(o2.getName().trim().toUpperCase(Locale.ROOT));
            if (j == null) {
                j = Integer.MAX_VALUE;
            }
            return i - j;
        });

        buList.add(prt);
        buList.add(mnt);
        buList.add(dt);
        customerList.add(lenovo);
        customerList.add(dell);
        customerList.add(hp);
        cumulativeOutputList.add(cumulativeOutputDtDell);
        cumulativeOutputList.add(cumulativeOutputDtHp);
        cumulativeOutputList.add(cumulativeOutputDtLenovo);
        cumulativeOutputList.add(cumulativeOutputMNT);
        cumulativeOutputList.add(cumulativeOutputPRT);
        productProjectOutputList.add(productProjectOutputDtDell);
        productProjectOutputList.add(productProjectOutputDtHP);
        productProjectOutputList.add(productProjectOutputDtLenovo);
        productProjectOutputList.add(productProjectOutputMNT);
        productProjectOutputList.add(productProjectOutputPRT);

        return R.success(rv);
    }

    @ApiOperation("查询客户上线情况报表")
    @GetMapping("/searchCustomerOnlineDetail")
    public R<ArrayList<CustomerProjectOnlineDetail>> searchCustomerOnlineDetail(String bu) throws CloneNotSupportedException {

        ArrayList<CustomerProjectOnlineDetail> customerList = new ArrayList<>();

        List<ReportEntity> list = new ArrayList<>();
        QueryEntity query = new QueryEntity();
        query.setBu(bu);
        List<ReportEntity> sourceList = projectReportService.queryData(query);
        sourceList.removeIf(entity -> entity.shouldOutputDeliverableQty == 0);
        ReportEntity lastPidReportEntity = new ReportEntity();
        for (ReportEntity reportEntity : sourceList) {
            String pid = reportEntity.pid;
            if (pid.equals(lastPidReportEntity.pid)) {
                lastPidReportEntity.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                lastPidReportEntity.outputDeliverableQty += reportEntity.outputDeliverableQty;
                lastPidReportEntity.archivedQty += reportEntity.archivedQty;
                lastPidReportEntity.shouldOutputDeliverableQty += reportEntity.shouldOutputDeliverableQty;
            } else {
                lastPidReportEntity = reportEntity.clone();
                list.add(lastPidReportEntity);
            }
        }

        CustomerProjectOnlineDetail lenovo = new CustomerProjectOnlineDetail("Lenovo");
        CustomerProjectOnlineDetail dell = new CustomerProjectOnlineDetail("Dell");
        CustomerProjectOnlineDetail hp = new CustomerProjectOnlineDetail("HP");
        if ("DT".equals(bu)) {
            customerList.add(lenovo);
            customerList.add(dell);
            customerList.add(hp);
        }

        for (ReportEntity reportEntity : list) {
            String customer = reportEntity.customer;
            String theBu = reportEntity.bu;
            int qty = reportEntity.workflowDiagramDocumentQty + reportEntity.outputDeliverableQty;
            if ("DT".equals(theBu)) {
                if (customer.startsWith("Lenovo")) {
                    if (qty > 0) {
                        lenovo.online++;
                    } else {
                        lenovo.notOnline++;
                    }
                    lenovo.total++;
                }
                if (customer.equals("Dell")) {
                    if (qty > 0) {
                        dell.online++;
                    } else {
                        dell.notOnline++;
                    }
                    dell.total++;
                }
                if (customer.equals("HP")) {
                    if (qty > 0) {
                        hp.online++;
                    } else {
                        hp.notOnline++;
                    }
                    hp.total++;
                }
            } else {
                CustomerProjectOnlineDetail e = findCustomerProjectOnlineDetail(customerList, customer);
                if (e == null) {
                    e = new CustomerProjectOnlineDetail(customer);
                    customerList.add(e);
                }
                if (qty > 0) {
                    e.online++;
                } else {
                    e.notOnline++;
                }
                e.total++;

            }

        }


        return R.success(customerList);
    }

    @ApiOperation("查询Function專案產出物情況")
    @GetMapping("/searchFunctionOnlineDetail")
    public R<List<FunctionOnlineDetail>> searchFunctionOnlineDetail(String group, String contentType, String groupBy) throws CloneNotSupportedException {
        List<FunctionOnlineDetail> functionOnlineDetailList = new ArrayList<>();
        QueryEntity query = new QueryEntity();
        if (group.contains("-")) {
            String[] split = group.split("-");
            query.setBu(split[0]);
            query.setCustomer(split[1]);
            query.setCustomerLike("是");
        } else {
            query.setBu(group);
        }
        List<ReportEntity> projectDeptList = new ArrayList<>();
        if ("function".equals(groupBy)) {
            List<ReportEntity> sourceList = projectReportService.queryData(query);
            sourceList.removeIf(entity -> entity.shouldOutputDeliverableQty == 0);
            for (ReportEntity reportEntity : sourceList) {
                String pid = reportEntity.pid;
                String dept = reportEntity.dept;
                ReportEntity projectDept = findProjectDept(projectDeptList, pid, dept);
                if (projectDept == null) {
                    projectDeptList.add(reportEntity.clone());
                } else {
                    projectDept.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                    projectDept.outputDeliverableQty += reportEntity.outputDeliverableQty;
                    projectDept.archivedQty += reportEntity.archivedQty;
                    projectDept.shouldOutputDeliverableQty += reportEntity.shouldOutputDeliverableQty;
                }
            }
            if ("onlineDetail".equals(contentType)) {
                for (ReportEntity reportEntity : projectDeptList) {
                    String dept = reportEntity.dept;
                    int qty = reportEntity.workflowDiagramDocumentQty + reportEntity.outputDeliverableQty;
                    FunctionOnlineDetail functionOnlineDetail = findFunctionOnlineDetail(functionOnlineDetailList, dept);
                    if (functionOnlineDetail == null) {
                        functionOnlineDetail = new FunctionOnlineDetail(dept);
                        functionOnlineDetailList.add(functionOnlineDetail);
                    }
                    functionOnlineDetail.totalQty++;
                    if (qty > 0) {
                        functionOnlineDetail.online++;
                    }
                    functionOnlineDetail.setRate(Float.parseFloat(String.format("%.2f", functionOnlineDetail.online * 1.0f / (functionOnlineDetail.totalQty == 0 ? 1 : functionOnlineDetail.totalQty) * 100)));
                }
            } else if ("cumulativeOutputList".equals(contentType)) {
                for (ReportEntity reportEntity : projectDeptList) {
                    String dept = reportEntity.dept;
                    FunctionOnlineDetail functionOnlineDetail = findFunctionOnlineDetail(functionOnlineDetailList, dept);
                    if (functionOnlineDetail == null) {
                        functionOnlineDetail = new FunctionOnlineDetail(dept);
                        functionOnlineDetailList.add(functionOnlineDetail);
                    }
                    functionOnlineDetail.outQty += reportEntity.outputDeliverableQty;
                    functionOnlineDetail.shouldOutQty += reportEntity.shouldOutputDeliverableQty;
                    functionOnlineDetail.setRate(Float.parseFloat(String.format("%.2f", functionOnlineDetail.outQty * 1.0f / (functionOnlineDetail.shouldOutQty == 0 ? 1 : functionOnlineDetail.shouldOutQty) * 100)));
                }
            } else if ("productProjectOutputList".equals(contentType)) {
                for (ReportEntity reportEntity : projectDeptList) {
                    String dept = reportEntity.dept;
                    FunctionOnlineDetail functionOnlineDetail = findFunctionOnlineDetail(functionOnlineDetailList, dept);
                    if (functionOnlineDetail == null) {
                        functionOnlineDetail = new FunctionOnlineDetail(dept);
                        functionOnlineDetailList.add(functionOnlineDetail);
                    }
                    functionOnlineDetail.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                    functionOnlineDetail.archivedQty += reportEntity.archivedQty;
                    functionOnlineDetail.setRate(Float.parseFloat(String.format("%.2f", functionOnlineDetail.archivedQty * 1.0f / (functionOnlineDetail.workflowDiagramDocumentQty == 0 ? 1 : functionOnlineDetail.workflowDiagramDocumentQty) * 100)));
                }
            }
            functionOnlineDetailList.sort((o1, o2) -> {
                Map<String, Integer> sort = sortRole.get(query.getBu());
                Integer i = sort.get(o1.getName().trim().toUpperCase(Locale.ROOT));
                if (i == null) {
                    i = Integer.MAX_VALUE;
                }
                Integer j = sort.get(o2.getName().trim().toUpperCase(Locale.ROOT));
                if (j == null) {
                    j = Integer.MAX_VALUE;
                }
                return i - j;
            });
            return R.success(functionOnlineDetailList);
        } else {
            // 产品线
            List<ReportEntity> sourceList = projectReportService.queryData(query);
            sourceList.removeIf(entity -> entity.shouldOutputDeliverableQty == 0);
            ;
            String bu = query.getBu();
            ReportEntity lastProductLineReportEntity = new ReportEntity();
            for (ReportEntity reportEntity : sourceList) {
                String pid = reportEntity.pid;
                String customer = reportEntity.customer;
                String productLine = reportEntity.productLine;
                boolean isSameGroup;
                if (query.getCustomer().isEmpty()) {
                    isSameGroup = productLine.equals(lastProductLineReportEntity.productLine) && pid.equals(lastProductLineReportEntity.pid);
                } else {
                    isSameGroup = productLine.equals(lastProductLineReportEntity.productLine) && pid.equals(lastProductLineReportEntity.pid) && customer.equals(lastProductLineReportEntity.customer);
                }
                if (isSameGroup) {
                    lastProductLineReportEntity.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                    lastProductLineReportEntity.outputDeliverableQty += reportEntity.outputDeliverableQty;
                    lastProductLineReportEntity.archivedQty += reportEntity.archivedQty;
                    lastProductLineReportEntity.shouldOutputDeliverableQty += reportEntity.shouldOutputDeliverableQty;
                } else {
                    lastProductLineReportEntity = reportEntity.clone();
                    projectDeptList.add(lastProductLineReportEntity);
                }
            }
            if ("onlineDetail".equals(contentType)) {
                for (ReportEntity reportEntity : projectDeptList) {
                    int qty = reportEntity.workflowDiagramDocumentQty + reportEntity.outputDeliverableQty;
                    String productLine = reportEntity.productLine;
                    String customer = reportEntity.customer;
                    String name;
                    if ("DT".equals(bu)) {
                        name = productLine;
                    } else {
                        name = customer + "-" + productLine;
                    }
                    FunctionOnlineDetail functionOnlineDetail = findFunctionOnlineDetail(functionOnlineDetailList, name);
                    if (functionOnlineDetail == null) {
                        functionOnlineDetail = new FunctionOnlineDetail(name);
                        functionOnlineDetailList.add(functionOnlineDetail);
                    }
                    functionOnlineDetail.totalQty++;
                    if (qty > 0) {
                        functionOnlineDetail.online++;
                    }
                    functionOnlineDetail.setRate(Float.parseFloat(String.format("%.2f", functionOnlineDetail.online * 1.0f / (functionOnlineDetail.totalQty == 0 ? 1 : functionOnlineDetail.totalQty) * 100)));
                }
            } else if ("cumulativeOutputList".equals(contentType)) {
                for (ReportEntity reportEntity : projectDeptList) {
                    String productLine = reportEntity.productLine;
                    String customer = reportEntity.customer;
                    String name;
                    if ("DT".equals(bu)) {
                        name = productLine;
                    } else {
                        name = customer + "-" + productLine;
                    }
                    FunctionOnlineDetail functionOnlineDetail = findFunctionOnlineDetail(functionOnlineDetailList, name);
                    if (functionOnlineDetail == null) {
                        functionOnlineDetail = new FunctionOnlineDetail(name);
                        functionOnlineDetailList.add(functionOnlineDetail);
                    }
                    functionOnlineDetail.outQty += reportEntity.outputDeliverableQty;
                    functionOnlineDetail.shouldOutQty += reportEntity.shouldOutputDeliverableQty;
                    functionOnlineDetail.setRate(Float.parseFloat(String.format("%.2f", functionOnlineDetail.outQty * 1.0f / (functionOnlineDetail.shouldOutQty == 0 ? 1 : functionOnlineDetail.shouldOutQty) * 100)));
                }
                return R.success(functionOnlineDetailList);
            } else if ("productProjectOutputList".equals(contentType)) {
                for (ReportEntity reportEntity : projectDeptList) {
                    String productLine = reportEntity.productLine;
                    String customer = reportEntity.customer;
                    String name;
                    if ("DT".equals(bu)) {
                        name = productLine;
                    } else {
                        name = customer + "-" + productLine;
                    }
                    FunctionOnlineDetail functionOnlineDetail = findFunctionOnlineDetail(functionOnlineDetailList, name);
                    if (functionOnlineDetail == null) {
                        functionOnlineDetail = new FunctionOnlineDetail(name);
                        functionOnlineDetailList.add(functionOnlineDetail);
                    }
                    functionOnlineDetail.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                    functionOnlineDetail.archivedQty += reportEntity.archivedQty;
                }
            }
        }
        return R.success(functionOnlineDetailList);
    }


    @ApiOperation("导出")
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody QueryEntity p) {
        HttpHeaders headers = new HttpHeaders();
        try {
            ByteArrayOutputStream export = e(p);
            headers.setContentDispositionFormData("attachment", "ProjectReport.xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<>(export.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(headers, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public ByteArrayOutputStream e(QueryEntity query) throws Exception {
        List<ReportEntity> list = new ArrayList<>();
        List<ReportEntity> sourceList = projectReportService.queryData(query);
        sourceList.removeIf(entity -> entity.shouldOutputDeliverableQty == 0);
        File destFile = FileUtil.releaseFile("ProjectVisualDashbord.xlsx");
        FileInputStream fis = new FileInputStream(Objects.requireNonNull(destFile));
        XSSFWorkbook wb = new XSSFWorkbook(fis);

        try {
            XSSFSheet sheet2 = wb.getSheetAt(1);
            projectReportService.writeSheet(sheet2, sourceList, wb, 1, true);
            String unique = "";
            for (ReportEntity reportEntity : sourceList) {
                String pid = reportEntity.pid;
                String historicalPhaseShort = reportEntity.historicalPhaseShort;
                if ((pid + historicalPhaseShort).equals(unique)) {
                    ReportEntity lastReportEntity = list.get(list.size() - 1);
                    lastReportEntity.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                    lastReportEntity.archivedQty += reportEntity.archivedQty;
                    lastReportEntity.outputDeliverableQty += reportEntity.outputDeliverableQty;
                    lastReportEntity.shouldOutputDeliverableQty += reportEntity.shouldOutputDeliverableQty;
                    lastReportEntity.outputProgress = Float.parseFloat(String.format("%.2f", lastReportEntity.outputDeliverableQty * 1.0f / (lastReportEntity.shouldOutputDeliverableQty == 0 ? 1 : lastReportEntity.shouldOutputDeliverableQty) * 100));
                } else {
                    unique = pid + historicalPhaseShort;
                    list.add(reportEntity);
                }
            }
            // 添加一个总计
            ReportEntity summery = new ReportEntity();
            summery.bu = "总计";
            summery.historicalPhaseShort = "总计";
            list.add(summery);
            // 将当前阶段放到最上面
            String curPid = "";
            int projectFirstIndex = 0;
            for (int i = 0; i < list.size(); i++) {
                ReportEntity reportEntity = list.get(i);
                String pid = reportEntity.pid;
                if (curPid.equals(pid)) {
                    if (reportEntity.phaseShort.equals(reportEntity.historicalPhaseShort)) {
                        ReportEntity firstEntity = list.get(projectFirstIndex);
                        list.set(projectFirstIndex, reportEntity);
                        moveDown(list, projectFirstIndex + 1, i, firstEntity);
                    }
                } else {
                    curPid = pid;
                    projectFirstIndex = i;
                }
            }
            // 按专案，阶段汇总
            curPid = "";
            projectFirstIndex = 0;
            int seriesFirstIndex = 0;
            int productLineFirstIndex = 0;
            int customerFirstIndex = 0;
            int buFirstIndex = 0;
            String curSeries = "";
            String curProductLine = "";
            String curCustomer = "";
            String curBu = "";
            List<ReportEntity> summaryList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                ReportEntity reportEntity = list.get(i);
                if (reportEntity != summery) {
                    summery.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
                    summery.archivedQty += reportEntity.archivedQty;
                    summery.outputDeliverableQty += reportEntity.outputDeliverableQty;
                    summery.shouldOutputDeliverableQty += reportEntity.shouldOutputDeliverableQty;
                }

                ReportEntity previousReportEntity = null;
                if (i > 0) {
                    previousReportEntity = list.get(i - 1);
                }
                String bu = reportEntity.bu;
                String customer = bu + reportEntity.customer;
                String productLine = customer + reportEntity.productLine;
                String series = productLine + reportEntity.series;
                String pid = reportEntity.pid;
                if (!curPid.equals(pid)) {
                    if (i > 0) {
                        // 历史阶段汇总
                        assert previousReportEntity != null:"previousReportEntity is null";
                        ReportEntity summary = summary(list, projectFirstIndex, i - 1, false);
                        summary.bu = previousReportEntity.bu;
                        summary.customer = previousReportEntity.customer;
                        summary.productLine = previousReportEntity.productLine;
                        summary.series = previousReportEntity.series;
                        summary.projectName = previousReportEntity.projectName;
                        summary.spm = previousReportEntity.spm;
                        summary.historicalPhaseShort = "歷史Phase 汇总";
                        summary.isSummery = true;
                        summary.index = i;
                        if (summary.shouldOutputDeliverableQty != 0) {
                            summaryList.add(summary);
                        }
                        // 专案汇总
                        assert previousReportEntity != null:"previousReportEntity is null";
                        summary = summary(list, projectFirstIndex, i - 1, true);
                        summary.bu = previousReportEntity.bu;
                        summary.customer = previousReportEntity.customer;
                        summary.productLine = previousReportEntity.productLine;
                        summary.series = previousReportEntity.series;
                        summary.projectName = previousReportEntity.projectName + "汇总";
                        summary.spm = "专案汇总";
                        summary.historicalPhaseShort = "专案汇总";
                        summary.isSummery = true;
                        summary.index = i;
                        if (summary.shouldOutputDeliverableQty != 0) {
                            summaryList.add(summary);
                        }
                    }
                    projectFirstIndex = i;
                    curPid = pid;
                }
                if (!curSeries.equals(series)) {
                    if (i > 0) {
                        // 系列汇总
                        assert previousReportEntity != null:"previousReportEntity is null";
                        ReportEntity summary = summary(list, seriesFirstIndex, i - 1, true);
                        summary.bu = previousReportEntity.bu;
                        summary.customer = previousReportEntity.customer;
                        summary.productLine = previousReportEntity.productLine;
                        summary.series = previousReportEntity.series + "汇总";
                        summary.projectName = "系列汇总";
                        summary.spm = "系列汇总";
                        summary.historicalPhaseShort = "系列汇总";
                        summary.isSummery = true;
                        summary.index = i;
                        if (summary.shouldOutputDeliverableQty != 0) {
                            summaryList.add(summary);
                        }
                    }
                    seriesFirstIndex = i;
                    curSeries = series;
                }
                if (!curProductLine.equals(productLine)) {
                    if (i > 0) {
                        // 产品线汇总
                        assert previousReportEntity != null:"previousReportEntity is null";
                        ReportEntity summary = summary(list, productLineFirstIndex, i - 1, true);
                        summary.bu = previousReportEntity.bu;
                        summary.customer = previousReportEntity.customer;
                        summary.productLine = previousReportEntity.productLine + "汇总";
                        summary.series = "产品线汇总";
                        summary.projectName = "产品线汇总";
                        summary.spm = "产品线汇总";
                        summary.historicalPhaseShort = "产品线汇总";
                        summary.index = i;
                        summary.isSummery = true;
                        if (summary.shouldOutputDeliverableQty != 0) {
                            summaryList.add(summary);
                        }
                    }
                    productLineFirstIndex = i;
                    curProductLine = productLine;
                }
                if (!curCustomer.equals(customer)) {
                    if (i > 0) {
                        // 客户汇总
                        assert previousReportEntity != null:"previousReportEntity is null";
                        ReportEntity summary = summary(list, customerFirstIndex, i - 1, true);
                        summary.bu = previousReportEntity.bu;
                        summary.customer = previousReportEntity.customer + "汇总";
                        summary.productLine = "客户汇总";
                        summary.series = "客户汇总";
                        summary.projectName = "客户汇总";
                        summary.spm = "客户汇总";
                        summary.historicalPhaseShort = "客户汇总";
                        summary.index = i;
                        summary.isSummery = true;
                        if (summary.shouldOutputDeliverableQty != 0) {
                            summaryList.add(summary);
                        }
                    }
                    customerFirstIndex = i;
                    curCustomer = customer;
                }
                if (!curBu.equals(bu)) {
                    if (i > 0) {
                        // BU汇总
                        assert previousReportEntity != null:"previousReportEntity is null";
                        ReportEntity summary = summary(list, buFirstIndex, i - 1, true);
                        summary.bu = previousReportEntity.bu + "汇总";
                        summary.customer = "BU汇总";
                        summary.productLine = "BU汇总";
                        summary.series = "BU汇总";
                        summary.projectName = "BU汇总";
                        summary.spm = "BU汇总";
                        summary.historicalPhaseShort = "BU汇总";
                        summary.index = i;
                        summary.isSummery = true;
                        if (summary.shouldOutputDeliverableQty != 0) {
                            summaryList.add(summary);
                        }
                    }
                    buFirstIndex = i;
                    curBu = bu;
                }
            }
            summery.outputProgress = Float.parseFloat(String.format("%.2f", summery.outputDeliverableQty * 1.0f / (summery.shouldOutputDeliverableQty == 0 ? 1 : summery.shouldOutputDeliverableQty) * 100));

            // 插入汇总
            int insertedQty = 0;
            for (ReportEntity reportEntity : summaryList) {
                list.add(reportEntity.index + insertedQty, reportEntity);
                insertedQty++;
            }

            XSSFSheet sheet = wb.getSheetAt(0);
            XSSFFont font = wb.createFont();
            font.setFontName("微软雅黑");
            XSSFFont fontBold = wb.createFont();
            fontBold.setFontName("微软雅黑");
            fontBold.setBold(true);
            XSSFCellStyle cellStyle = wb.createCellStyle();
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            cellStyle.setWrapText(true);
            cellStyle.setFont(font);
            cellStyle.setBorderBottom(BorderStyle.THIN); //下边框
            cellStyle.setBorderLeft(BorderStyle.THIN);//左边框
            cellStyle.setBorderTop(BorderStyle.THIN);//上边框
            cellStyle.setBorderRight(BorderStyle.THIN);//右边框
            cellStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(255, 255, 255), new DefaultIndexedColorMap()));
            XSSFCellStyle topStyle = wb.createCellStyle();
            topStyle.cloneStyleFrom(cellStyle);
            topStyle.setVerticalAlignment(VerticalAlignment.TOP);
            XSSFCellStyle projectStyle = wb.createCellStyle();
            projectStyle.cloneStyleFrom(cellStyle);
            projectStyle.setFont(fontBold);
            projectStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(220, 230, 241), new DefaultIndexedColorMap()));
            XSSFCellStyle seriesStyle = wb.createCellStyle();
            seriesStyle.cloneStyleFrom(cellStyle);
            seriesStyle.setFont(fontBold);
            seriesStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(197, 217, 241), new DefaultIndexedColorMap()));
            XSSFCellStyle productLineStyle = wb.createCellStyle();
            productLineStyle.cloneStyleFrom(cellStyle);
            productLineStyle.setFont(fontBold);
            productLineStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(141, 180, 226), new DefaultIndexedColorMap()));
            XSSFCellStyle customerStyle = wb.createCellStyle();
            customerStyle.cloneStyleFrom(cellStyle);
            customerStyle.setFont(fontBold);
            customerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(83, 141, 213), new DefaultIndexedColorMap()));
            XSSFCellStyle buStyle = wb.createCellStyle();
            buStyle.cloneStyleFrom(cellStyle);
            buStyle.setFont(fontBold);
            buStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(79, 129, 189), new DefaultIndexedColorMap()));
            XSSFCellStyle summeryStyle = wb.createCellStyle();
            XSSFFont fontBoldWhite = wb.createFont();
            fontBoldWhite.setBold(true);
            fontBoldWhite.setColor(new XSSFColor(new java.awt.Color(255, 255, 255), new DefaultIndexedColorMap()));
            fontBoldWhite.setFontName("微软雅黑");
            summeryStyle.cloneStyleFrom(cellStyle);
            summeryStyle.setFont(fontBoldWhite);
            summeryStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(0, 112, 192), new DefaultIndexedColorMap()));
            for (int i = 0; i < list.size(); i++) {
                ReportEntity entity = list.get(i);
                int qty = entity.workflowDiagramDocumentQty + entity.outputDeliverableQty;
                XSSFRow row = sheet.createRow(i + 3);
                ExcelUtil.setCellStyleAndValue(row.createCell(0), entity.bu, topStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(1), entity.customer, topStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(2), entity.productLine, topStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(3), entity.series, topStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(4), entity.projectName, cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(5), entity.spm, cellStyle);
                if (entity.historicalPhaseShort.endsWith("汇总")) {
                    ExcelUtil.setCellStyleAndValue(row.createCell(6), entity.historicalPhaseShort, cellStyle);
                }
                if (!entity.isSummery) {
                    String value = entity.phaseShort.equals(entity.historicalPhaseShort) ? "當前Phase" : "歷史Phase";
                    entity.phaseType = value;
                    ExcelUtil.setCellStyleAndValue(row.createCell(6), value, cellStyle);
                }
                ExcelUtil.setCellStyleAndValue(row.createCell(7), entity.historicalPhaseShort, cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(8), entity.workflowDiagramDocumentQty, cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(9), entity.archivedQty, cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(10), entity.outputDeliverableQty, cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(11), entity.shouldOutputDeliverableQty, cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(12), entity.outputProgress + "%", cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(13), qty > 0 ? "已上線" : "未上線", cellStyle);
            }
            int startLine = 3;
            List<MegerCellEntity> megerCellList0 = ExcelUtil.scanMegerCells2(list, "bu", startLine);
            List<MegerCellEntity> megerCellList1 = ExcelUtil.scanMegerCells2(list, "customer", startLine);
            List<MegerCellEntity> megerCellList2 = ExcelUtil.scanMegerCells2(list, "productLine", startLine);
            List<MegerCellEntity> megerCellList3 = ExcelUtil.scanMegerCells2(list, "series", startLine);
            List<MegerCellEntity> megerCellList4 = ExcelUtil.scanMegerCells2(list, "projectName", startLine);
            List<MegerCellEntity> megerCellList5 = ExcelUtil.scanMegerCells2(list, "spm", startLine);
            List<MegerCellEntity> megerCellList6 = new ArrayList<>();
            int startRow = 0;
            int count = 0;
            for (int i = 0; i < list.size(); i++) {
                ReportEntity reportEntity = list.get(i);
                String phaseType = reportEntity.phaseType;
                if ("歷史Phase".equals(phaseType)) {
                    if (count == 0) {
                        startRow = i + startLine;
                    }
                    count++;
                } else {
                    if (count > 1) {
                        megerCellList6.add(new MegerCellEntity(startRow, startRow + count - 1));
                    }
                    startRow = 0;
                    count = 0;
                }
            }
            //合并单位格
            for (MegerCellEntity megerCellEntity : megerCellList0) {
                sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 0, 0));
            }
            for (MegerCellEntity megerCellEntity : megerCellList1) {
                sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 1, 1));
            }
            for (MegerCellEntity megerCellEntity : megerCellList2) {
                sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 2, 2));
            }
            for (MegerCellEntity megerCellEntity : megerCellList3) {
                sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 3, 3));
            }
            for (MegerCellEntity megerCellEntity : megerCellList4) {
                sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 4, 4));
            }
            for (MegerCellEntity megerCellEntity : megerCellList5) {
                sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 5, 5));
            }
            for (MegerCellEntity megerCellEntity : megerCellList6) {
                sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 6, 6));
            }
            for (int i = 0; i < list.size(); i++) {
                ReportEntity reportEntity = list.get(i);
                String historicalPhaseShort = reportEntity.historicalPhaseShort;
                if (!historicalPhaseShort.endsWith("汇总") && !historicalPhaseShort.endsWith("总计")) {
                    continue;
                }
                if ("歷史Phase 汇总".equals(historicalPhaseShort)) {
                    sheet.addMergedRegion(new CellRangeAddress(i + startLine, i + startLine, 6, 7));
                }
                if ("专案汇总".equals(historicalPhaseShort)) {
                    sheet.addMergedRegion(new CellRangeAddress(i + startLine, i + startLine, 4, 7));
                    sheet.getRow(i + startLine).getCell(4).setCellStyle(projectStyle);
                    for (int j = 8; j < 14; j++) {
                        sheet.getRow(i + startLine).getCell(j).setCellStyle(projectStyle);
                    }
                }
                if ("系列汇总".equals(historicalPhaseShort)) {
                    sheet.addMergedRegion(new CellRangeAddress(i + startLine, i + startLine, 3, 7));
                    sheet.getRow(i + startLine).getCell(3).setCellStyle(seriesStyle);
                    for (int j = 8; j < 14; j++) {
                        sheet.getRow(i + startLine).getCell(j).setCellStyle(seriesStyle);
                    }
                }
                if ("产品线汇总".equals(historicalPhaseShort)) {
                    sheet.addMergedRegion(new CellRangeAddress(i + startLine, i + startLine, 2, 7));
                    sheet.getRow(i + startLine).getCell(2).setCellStyle(productLineStyle);
                    for (int j = 8; j < 14; j++) {
                        sheet.getRow(i + startLine).getCell(j).setCellStyle(productLineStyle);
                    }
                }
                if ("客户汇总".equals(historicalPhaseShort)) {
                    sheet.addMergedRegion(new CellRangeAddress(i + startLine, i + startLine, 1, 7));
                    sheet.getRow(i + startLine).getCell(1).setCellStyle(customerStyle);
                    for (int j = 8; j < 14; j++) {
                        sheet.getRow(i + startLine).getCell(j).setCellStyle(customerStyle);
                    }
                }
                if ("BU汇总".equals(historicalPhaseShort)) {
                    sheet.addMergedRegion(new CellRangeAddress(i + startLine, i + startLine, 0, 7));
                    sheet.getRow(i + startLine).getCell(0).setCellStyle(buStyle);
                    for (int j = 8; j < 14; j++) {
                        sheet.getRow(i + startLine).getCell(j).setCellStyle(buStyle);
                    }
                }
                if ("总计".equals(historicalPhaseShort)) {
                    sheet.addMergedRegion(new CellRangeAddress(i + startLine, i + startLine, 0, 7));
                    sheet.getRow(i + startLine).getCell(0).setCellStyle(summeryStyle);
                    for (int j = 8; j < 14; j++) {
                        sheet.getRow(i + startLine).getCell(j).setCellStyle(summeryStyle);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            out.flush();
            destFile.delete();
            return out;
        } finally {

            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                wb.close(); // 关闭此对象，便于后续删除此文件
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void moveDown(List<ReportEntity> list, int start, int end, ReportEntity firstEntity) {

        ReportEntity cur = list.get(start);
        list.set(start, firstEntity);
        for (int i = start + 1; i <= end; i++) {
            ReportEntity reportEntity = list.get(i);
            list.set(i, cur);
            cur = reportEntity;
        }

    }

    private FunctionOnlineDetail findFunctionOnlineDetail(List<FunctionOnlineDetail> list, String function) {
        for (FunctionOnlineDetail functionOnlineDetail : list) {
            if (functionOnlineDetail.getName().equals(function)) {
                return functionOnlineDetail;
            }
        }
        return null;
    }

    private ReportEntity findProjectDept(List<ReportEntity> list, String pid, String dept) {
        for (ReportEntity item : list) {
            if (item.pid.equals(pid) && item.dept.equals(dept)) {
                return item;
            }
        }
        return null;
    }

    private CustomerProjectOnlineDetail findCustomerProjectOnlineDetail(List<CustomerProjectOnlineDetail> list, String name) {
        for (CustomerProjectOnlineDetail customerProjectOnlineDetail : list) {
            if (name.equals(customerProjectOnlineDetail.getName())) {
                return customerProjectOnlineDetail;
            }
        }
        return null;
    }

    private ReportEntity summary(List<ReportEntity> list, int start, int end, boolean containsCurPhase) {
        ReportEntity summary = new ReportEntity();
        for (int i = start; i <= end; i++) {
            ReportEntity reportEntity = list.get(i);

            if (reportEntity.shouldOutputDeliverableQty == 0) {
                continue;
            }

            if (reportEntity.phaseShort.equals(reportEntity.historicalPhaseShort)) {
                if (!containsCurPhase) {
                    continue;
                }
            }


            summary.workflowDiagramDocumentQty += reportEntity.workflowDiagramDocumentQty;
            summary.archivedQty += reportEntity.archivedQty;
            summary.outputDeliverableQty += reportEntity.outputDeliverableQty;
            summary.shouldOutputDeliverableQty += reportEntity.shouldOutputDeliverableQty;
        }
        summary.outputProgress = Float.parseFloat(String.format("%.2f", summary.outputDeliverableQty * 1.0f / (summary.shouldOutputDeliverableQty == 0 ? 1 : summary.shouldOutputDeliverableQty) * 100));
        return summary;
    }

}
