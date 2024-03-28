package com.foxconn.plm.tcservice.projectReport;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.matrix.service.impl.MatrixServiceImpl;
import com.foxconn.plm.tcservice.projectReport.dto.rv.DeptEmailBean;
import com.foxconn.plm.tcservice.projectReport.dto.rv.SPMEmailBean;
import com.foxconn.plm.utils.file.FileUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.fileupload.FileItem;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Component
public class EmailNoticeTask {

    private static final Log log = LogFactory.get();

    @Resource
    TcMailClient tcMailClient;


    @Resource
    TcIntegrateClient tcIntegrateClient;

    @Resource
    ProjectReportService service;

    @Resource
    MatrixServiceImpl matrixService;

    @Resource
    Environment environment;

//    @PostConstruct
    @XxlJob("sendTracEmail")
    public void sendTracEmail() throws Exception {
        log.info("==>> 【Start】專案執行報表郵件跟催任務");
        tcIntegrateClient.freshReport();
        List<String> deptSpmCCList = new ArrayList<>();
        List<String> noEmailNoticeList = new ArrayList<>();
        List<String> noEmailCCList = new ArrayList<>();
        // 登錄TC
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        PreferenceManagementService preferenceManagementService = tcsoaServiceFactory.getPreferenceManagementService();
        DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
        FileManagementUtility fileManagementUtility = tcsoaServiceFactory.getFileManagementUtility();
        String[] configUid = TCUtils.getTCPreferences(preferenceManagementService, "D9_Project_Report_Email_Address_Excel_UID");
        Dataset dataset = (Dataset) TCUtils.findObjectByUid(dataManagementService, configUid[0]);
        String tmpdir = System.getProperty(Access.check("java.io.tmpdir"));
        File file = TCUtils.downloadDataset(dataManagementService, fileManagementUtility, dataset, tmpdir);
        // 登出TC
        tcsoaServiceFactory.logout();
        assert file != null;
        List<DeptEmailBean> deptEmailList = null;
        List<SPMEmailBean> spmEmailList = null;
        ExcelReader reader = null;
        try {
            reader = ExcelUtil.getReader(file);
            reader.setSheet("郵件發送Function List");
            int lastRow = reader.getRowCount();
            deptEmailList = new ArrayList<>();
            for (int i = 1; i < lastRow; i++) {
                String bu = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(0, i));
                String customer = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(1, i));
                String dept = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(2, i));
                String username = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(3, i));
                String email = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(4, i)).trim();
                String filePath = System.getProperty("user.dir") + "/release/"+"Teamcenter專案執行報表Function未達成明細-"+dept.replace("/"," ")+".xlsx";
                deptEmailList.add(new DeptEmailBean(bu,customer,dept,username,email,filePath));
            }
            reader.setSheet("郵件發送 SPM List");

            lastRow =reader.getRowCount();
            spmEmailList = new ArrayList<>();
            for (int i = 1; i < lastRow; i++) {
                String bu = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(0, i));
                String customer = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(1, i));
                String spm = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(2, i));
                String email = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(3, i)).trim();
                String filePath = System.getProperty("user.dir") + "/release/"+"Teamcenter專案執行報表Function未達成明細-SPM.xlsx";
                spmEmailList.add(new SPMEmailBean(bu,customer,spm,email,filePath));
            }
            reader.setSheet("當前時間配置");
            ProjectReportService.emailTrackCurrentDate = Integer.parseInt(com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(0, 0)));
            reader.setSheet("郵箱未配置通知");
            lastRow =reader.getRowCount();
            for (int i = 1; i < lastRow; i++) {
                String email1 = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(0, i));
                String email2 = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(1, i));
                String email3 = com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(2, i));
                if(email1 != null && email1.contains("@")){
                    deptSpmCCList.add(email1);
                }
                if(email2 != null && email2.contains("@")){
                    noEmailNoticeList.add(email2);
                }
                if(email3 != null && email3.contains("@")){
                    noEmailCCList.add(email3);
                }
            }
            reader.close();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }finally {
            try{
                if(reader !=null){
                  reader.close();
                }
            }catch(Exception e){}

        }
        List<ReportEntity> reportList = service.queryData(new QueryEntity());
        List<ReportEntity> list = new ArrayList<>();
        for (DeptEmailBean deptEmailBean : deptEmailList) {
            list.clear();
            for (ReportEntity reportEntity : reportList) {
                if(reportEntity.outputProgress == 100f){
                    continue;
                }
                if (!reportEntity.isNeedComplete) {
                    continue;
                }
                if(!deptEmailBean.getBu().trim().equals(reportEntity.bu.trim())&&!"/".equals(deptEmailBean.getBu().trim())){
                    continue;
                }
                if(!deptEmailBean.getCustomer().trim().equals(reportEntity.customer.trim())&&!"/".equals(deptEmailBean.getCustomer().trim())){
                    continue;
                }
                if(!deptEmailBean.getDept().trim().equals(reportEntity.dept.trim())&&!"/".equals(deptEmailBean.getDept().trim())){
                    continue;
                }
                list.add(reportEntity);
            }
            if (list.isEmpty()) {
                continue;
            }
            File excelTemplate = FileUtil.releaseFile("ProjectReportExportTemplate.xlsx");
            File destFile = new File(deptEmailBean.getFilePath());
            export(list,excelTemplate,destFile);
            System.out.println(destFile.getAbsolutePath());
            // 發送郵件
            String subject = String.format("【提醒%s/%s】請及時在Teamcenter完成專案交付物產出與發佈",deptEmailBean.getCustomer(),deptEmailBean.getDept());
            sendM(deptEmailBean.getEmail(), "以上附件為本週上線專案協作率未達成的明細，請各位Key User及時提醒相關專案成員登錄Teamcenter正式機或SPAS外掛系統進行專案交付物上傳及發佈，謝謝！", destFile, deptEmailBean.getUsername(),"",deptSpmCCList,subject);
        }
        for (SPMEmailBean spmEmailBean : spmEmailList) {
            list.clear();
            for (ReportEntity reportEntity : reportList) {
                if(reportEntity.outputProgress == 100f){
                    continue;
                }
                if (!reportEntity.isNeedComplete) {
                    continue;
                }
                if(!spmEmailBean.getBu().trim().equals(reportEntity.bu.trim())&&!"/".equals(spmEmailBean.getBu().trim())){
                    continue;
                }
                if(!spmEmailBean.getCustomer().trim().equals(reportEntity.customer.trim())&&!"/".equals(spmEmailBean.getCustomer().trim())){
                    continue;
                }
                if(!spmEmailBean.getSpm().trim().equals(reportEntity.spm.trim())&&!"/".equals(spmEmailBean.getSpm().trim())){
                    continue;
                }
                list.add(reportEntity);
            }
            if (list.isEmpty()) {
                continue;
            }
            File excelTemplate = FileUtil.releaseFile("ProjectReportExportTemplate.xlsx");
            File destFile = new File(spmEmailBean.getFilePath());
            export(list,excelTemplate,destFile);
            System.out.println(destFile.getAbsolutePath());
            // 發送郵件
            sendM(spmEmailBean.getEmail(), "以上附件為本週上線專案協作率未達成的明細，請各位SPM及時提醒相關專案成員登錄Teamcenter正式機或SPAS外拐系統進行專案交付物上傳及發佈，謝謝！", destFile, spmEmailBean.getSpm(),spmEmailBean.getCustomer(),deptSpmCCList,"【提醒】請及時在Teamcenter完成專案交付物產出與歸檔");
        }
        List<String> noEmailSpmList = new ArrayList<>();
        for (ReportEntity reportEntity : reportList) {
            if(reportEntity.outputProgress == 100f){
                continue;
            }
            if (!reportEntity.isNeedComplete) {
                continue;
            }
            String flag1 = reportEntity.bu.trim() +"/"+ reportEntity.customer.trim() +"/"+ reportEntity.spm.trim();
            if(flag1.endsWith("spas管理员")){
                continue;
            }
            boolean found = false;
            for (SPMEmailBean spmEmailBean : spmEmailList) {
                String flag2 = spmEmailBean.getBu().trim() +"/"+ spmEmailBean.getCustomer().trim() +"/"+ spmEmailBean.getSpm().trim();
                if(flag1.equals(flag2)){
                    found = true;
                    break;
                }
            }
            if(!found){
                // 未配置郵箱的SPM
                if(!noEmailSpmList.contains(flag1)){
                    noEmailSpmList.add(flag1);
                }
            }
        }

        if (!noEmailSpmList.isEmpty()) {
            StringBuilder sBuilder = new StringBuilder();
            for (String spm : noEmailSpmList) {
                sBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;").append(spm).append("<br>");
            }
            noEmailNotice(noEmailNoticeList,"以下SPM未配置提醒郵箱，請注意：<br><br>"+sBuilder.toString(),noEmailCCList);
        }
        log.info("<<==【End】專案執行報表郵件跟催任務");
    }

    public void export(List<ReportEntity> list,File excelTemplate,File destFile) throws Exception {
        XSSFWorkbook wb = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(excelTemplate);
            wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheetAt(0);
            service.writeSheet(sheet, list, wb, 2, false);
            fos = new FileOutputStream(destFile);
            wb.write(fos);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            }catch(IOException e){}
            try {
                if (fos != null) {
                    fos.close();
                }
            }catch(IOException e){}
            try {
                if (wb != null) {
                    wb.close();
                }
            }catch (IOException e){}
        }
    }

    private void sendM(String to,String content,File excelFile,String people,String customer,List<String> cc,String subject) throws InterruptedException {
        String body = "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\"\n" +
                "          content=\"width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\">\n" +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" +
                "    <title>Document</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "\tDear "+people+",  \n" +
                "\t<br><br>\n" +
                "\t&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+content+"\n" +
                "\t<br><br>\n" +
                "\t<h3 style=\"margin: 0\">Teamcenter 系统自动定时发送,请勿回复邮件！。<br>如有問題，請聯繫吳敏mindy.m.wu@foxconn.com / 田馨tian.x.tian@foxconn.com</h3>\n" +
                "\t\n" +
                "\n" +
                "</body>\n" +
                "</html>";
        Map<String, String> httpmap = new HashMap<>();
        httpmap.put("sendTo", to);
        if(!cc.isEmpty()){
            httpmap.put("sendCc", ArrayUtil.join(cc.toArray(),","));
        }
        httpmap.put("subject", subject);
        httpmap.put("htmlmsg", body);
        Gson gson = new Gson();
        String data = gson.toJson(httpmap);
        FileItem file = matrixService.createFileItem(excelFile, "file");
        log.info("發送郵件到"+to);
        String s = tcMailClient.sendMail3Method(data,new CommonsMultipartFile(file));
        if (!"success".equalsIgnoreCase(s)) {
            log.info("郵件發送失败: X "+s);
        }else {
            log.info("郵件發送成功 √");
        }
        Thread.sleep(5000);
        boolean delete = excelFile.delete();
        if(delete){
            log.info("緩存文件刪除成功 √");
        }else {
            log.info("緩存文件刪除失敗 X");
        }
    }

    private void noEmailNotice(List<String> to,String content,List<String> cc) {
        String body = "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\"\n" +
                "          content=\"width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\">\n" +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" +
                "    <title>Document</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "\tDear "+"管理員"+",  \n" +
                "\t<br><br>\n" +
                "\t&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+content+"\n" +
                "\t<br><br>\n" +
                "\t<h3 style=\"margin: 0\">Teamcenter 系统自动定时发送,请勿回复邮件！。</h3>\n" +
                "\t\n" +
                "\n" +
                "</body>\n" +
                "</html>";
        Map<String, String> httpmap = new HashMap<>();
        httpmap.put("sendTo", ArrayUtil.join(to.toArray(),","));
        if(!cc.isEmpty()){
            httpmap.put("sendCc", ArrayUtil.join(cc.toArray(),","));
        }
        httpmap.put("subject", "【提醒】專案執行達成率SPM郵箱未配置");
        httpmap.put("htmlmsg", body);
        Gson gson = new Gson();
        String data = gson.toJson(httpmap);
        log.info("發送郵件到"+to);
        String s = tcMailClient.sendMail3Method(data);
        if (!"success".equalsIgnoreCase(s)) {
            log.info("郵件發送失败: X "+s);
        }else {
            log.info("郵件發送成功 √");
        }
    }

}
