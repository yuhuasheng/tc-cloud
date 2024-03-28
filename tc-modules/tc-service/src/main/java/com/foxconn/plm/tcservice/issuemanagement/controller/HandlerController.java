package com.foxconn.plm.tcservice.issuemanagement.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCDatasetEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.DatasetUtil;
import com.foxconn.plm.utils.tc.SessionUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 問題管理 提交到PRA功能
 *
 * @Description
 * @Author MW00442
 * @Date 2023/11/21 16:33
 **/
@RestController
@RequestMapping("/issueManagement")
@RefreshScope
public class HandlerController {
    @Resource
    private TCSOAServiceFactory tcsoaServiceFactory;
    @Value("${sftp.host}")
    private String host;
    @Value("${sftp.port}")
    private Integer port;
    @Value("${sftp.user}")
    private String user;
    @Value("${sftp.secretKey}")
    private String secretKey;

    @ApiOperation("上傳文件到對應的ftp中")
    @PostMapping("/submitToRpa")
    public R submitToRpa(@RequestBody List<String> uids) {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        for (String uid : uids) {
            ModelObject obj = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), uid);
            if(ObjectUtil.isNull(obj)){
                continue;
            }
            InputStream is = null;
            File file = null;
            FileOutputStream out = null;
            FileInputStream fis = null;

            try{

                TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(),obj,"object_type");
                String type = obj.getPropertyDisplayableValue("object_type");
                LogFactory.get().info("-------------------" + type);
                if(!"IR DELL Revision".equals(type) && !"IR HP Revision".equals(type) &&  !"IR LENOVO Revision".equals(type)){
                    continue;
                }
                ClassPathResource cpr = new ClassPathResource("/templates/Dell Issue RPA Temp.xlsx");
                String path = "";
                String[] propertities = null;
                if("IR DELL Revision".equals(type)){
                    path = "/Dell Issue/";
                    propertities = new String[] {"d9_ActualUserID","d9_IRAnalyst","d9_IRCustomerIssueNumber","item_id","d9_IRStatus","d9_IROriginatingVendor",
                            "d9_IROriginatingGroup","d9_IRLOBFound","d9_IRPlatformFoundDell","d9_IRCategory","d9_IRGroupActivity","d9_IRGroupLocation",
                            "d9_IRPhaseFoundDell","d9_IRHardwareBuildVersion","d9_IRDiscoveryMethod","d9_IRTestCaseNumberRequired","d9_IRCommodity","d9_IRComponent",
                            "d9_IRProductImpact","d9_IRCustomerImpactDell","d9_IRLikelihood","d9_IRAffectedOS","d9_IRAffectedLanguages","d9_IRAffectedItemsDell",
                            "d9_IRName","d9_IRLongDescription","d9_IRStepsToReproduce"};
                }else if("IR HP Revision".equals(type)){
                    path = "/HP Issue/";
                    propertities = new String[] {"d9_ActualUserID","d9_IRAnalyst","d9_IRCustomerIssueNumber","item_id","d9_IRStatus","d9_IROriginatingVendor",
                            "d9_IROriginatingGroup","d9_IRLOBFound","d9_IRPlatformFoundDell","d9_IRCategory","d9_IRGroupActivity","d9_IRGroupLocation",
                            "d9_IRPhaseFoundDell","d9_IRHardwareBuildVersion","d9_IRDiscoveryMethod","d9_IRTestCaseNumberRequired","d9_IRCommodity","d9_IRComponent",
                            "d9_IRProductImpact","d9_IRCustomerImpactDell","d9_IRLikelihood","d9_IRAffectedOS","d9_IRAffectedLanguages","d9_IRAffectedItems",
                            "d9_IRName","d9_IRLongDescription","d9_IRStepsToReproduce"};
                }else if("IR LENOVO Revision".equals(type)){
                    path = "/Lenovo Issue/";
                    propertities = new String[] {"d9_ActualUserID","d9_IRAnalyst","d9_IRCustomerIssueNumber","item_id","d9_IRStatus","d9_IROriginatingVendor",
                            "d9_IROriginatingGroup","d9_IRLOBFound","d9_IRPlatformFoundDell","d9_IRCategory","d9_IRGroupActivity","d9_IRGroupLocation",
                            "d9_IRPhaseFoundDell","d9_IRHardwareBuildVersion","d9_IRDiscoveryMethod","d9_IRTestCaseNumberRequired","d9_IRCommodity","d9_IRComponent",
                            "d9_IRProductImpact","d9_IRCustomerImpactDell","d9_IRLikelihood","d9_IRAffectedOS","d9_IRAffectedLanguages","d9_IRAffectedItems",
                            "d9_IRName","d9_IRLongDescription","d9_IRStepsToReproduce"};
                }
                is = cpr.getInputStream();
                XSSFWorkbook workbook = new XSSFWorkbook(is);
                XSSFCellStyle cellStyle = workbook.getCellStyleAt(0);
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                cellStyle.setWrapText(true);
                cellStyle.setBorderBottom(BorderStyle.THIN);
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderTop(BorderStyle.THIN);
                cellStyle.setBorderRight(BorderStyle.THIN);
                Set<String> set = CollUtil.newHashSet("d9_IRLOBFound","d9_IRPlatformFoundDell","d9_IRAffectedOS","d9_IRAffectedLanguages","d9_IRAffectedItems","d9_IRAffectedItemsDell");
                TCUtils.getProperties(tcsoaServiceFactory.getDataManagementService(),obj,propertities);
                TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(),obj);
                XSSFSheet sheet = workbook.getSheetAt(0);
                XSSFRow row = sheet.createRow(1);
                for (int i = 0; i < propertities.length; i++) {
                    String value = null;
                    if(set.contains(propertities[i])){
                        List<String> values = obj.getPropertyDisplayableValues(propertities[i]);
                        if("d9_IRAffectedOS".equals(propertities[i])){
                            values = values.stream().map(item ->{
                                int index = item.indexOf(",");
                                if(index != -1 &&item.length() > index +1 &&  ' ' != item.charAt(index +1)){
                                    return  item.replace(",",", ");
                                }
                                return item;
                            }).collect(Collectors.toList());
                            value = CollUtil.join(values,";");
                        } else {
                            value = CollUtil.join(values,",");
                        }
                    } else {
                        value = obj.getPropertyDisplayableValue(propertities[i]);
                    }
                    if(StrUtil.isNotBlank(value) && (i == 18 || i == 19 || i == 20)){
                        value = value.substring(0,value.indexOf("|"));
                    }
                    setCellValue(row,i,cellStyle,value);
                }
                String itemId = obj.getPropertyDisplayableValue("item_id");
                file = new File(itemId + ".xlsx");
                out = new FileOutputStream(file);
                workbook.write(out);
                out.flush();

                ItemRevision itemRevision = (ItemRevision) obj;
                // 創建數據集
                Dataset dataset = DatasetUtil.createDataset(tcsoaServiceFactory.getDataManagementService(), itemRevision,
                        itemId + ".xlsx", TCDatasetEnum.MSExcelX.type(), TCDatasetEnum.MSExcelX.relationType());
                DatasetUtil.addDatasetFile(tcsoaServiceFactory.getFileManagementUtility(),tcsoaServiceFactory.getDataManagementService(),
                        dataset,file,TCDatasetEnum.MSExcelX.refName(),false);


                Sftp sftp = JschUtil.createSftp(host, port, user, secretKey);
                if(!sftp.exist(path + itemId)) {
                    sftp.cd(path.substring(0,path.length()-1));
                    sftp.mkdir(itemId);
                }
                fis = new FileInputStream(file);
                sftp.upload(path + "Issue List", itemId+ ".xlsx",fis);

                // 加載關係
                String[] relateList = new String[] {"ImageBeforeFix","CMReferences","CMHasProblemItem","IsM0SnapshotBeforeFix","IsM0SnapshotAfterFix",
                        "IsM0IssueSubset","CMHasImpactedItem","CMReferences","ImageAfterFix"};
                TCUtils.getProperties(tcsoaServiceFactory.getDataManagementService(),obj,relateList);
                TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(),obj);
                for (int i = 0; i < relateList.length; i++) {
                    List<ModelObject> listObj = obj.getPropertyObject(relateList[i]).getModelObjectListValue();
                    for (ModelObject modelObject : listObj) {
                        String objType = modelObject.getTypeObject().getName();
                        if(!objType.equals("DataSet")){
                            File[] dataSetFiles = DatasetUtil.getDataSetFiles(tcsoaServiceFactory.getDataManagementService(), (Dataset) modelObject, tcsoaServiceFactory.getFileManagementUtility());
                            for (File dataSetFile : dataSetFiles) {
                                FileInputStream fileInputStream = null;
                                try {
                                    TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), modelObject, "object_name");
                                    TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), modelObject);
                                    String objectName = ((Dataset) modelObject).get_object_name();
                                    if (!sftp.exist(path + itemId + "/" + objectName)) {
                                        fileInputStream = new FileInputStream(dataSetFile);
                                        sftp.upload(path + itemId, objectName, fileInputStream);
                                    }
                                }finally {
                                    try {
                                        if (fileInputStream != null) {
                                            fileInputStream.close();
                                        }
                                    }catch(IOException e){}
                                }
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                LogFactory.get().error("上傳文件到Sftp錯誤，對象id為：" + uid);
            }finally {
                try {
                    if (is != null) {
                       is.close();
                    }
                }catch(IOException e){}

                try {
                    if (out != null) {
                       out.close();
                    }
                }catch (IOException e){}
                try {
                    if (fis != null) {
                        fis.close();
                    }
                }catch (IOException e){}
                if(ObjectUtil.isNotNull(file)){
                    FileUtil.del(file);
                }
            }
        }
        tcsoaServiceFactory.logout();
        return R.success(true);
    }

    private void setCellValue(XSSFRow row,int cellIndex,XSSFCellStyle cellStyle,String value) {
        XSSFCell cell = row.createCell(cellIndex);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(value);
    }
}
