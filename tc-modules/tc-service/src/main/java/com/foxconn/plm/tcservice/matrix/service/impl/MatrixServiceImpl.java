package com.foxconn.plm.tcservice.matrix.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.constants.TCProjectConstant;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.pojo.ActualUserPojo;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.matrix.service.MatrixService;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.ActualUserUtil;
import com.foxconn.plm.utils.tc.DatasetUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.ImanFile;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatrixServiceImpl implements MatrixService {

    private static Log log = LogFactory.get();

    @Resource
    private TcMailClient tcMailClient;

    public String sendMatrixEmailBackup(String taskName, String subject, String currentName, String changeDesc, String firstTargetUid,String attachments) {
        TCSOAServiceFactory tcsoaServiceFactory = null;
        try {
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
            FileManagementUtility fileManagementUtility = tcsoaServiceFactory.getFileManagementUtility();
            ItemRevision firstTarget = (ItemRevision) TCUtils.findObjectByUid(dataManagementService, firstTargetUid);
            List<String> sentToList = parseEmail(dataManagementService, firstTarget, fileManagementUtility, currentName);
            if (sentToList.isEmpty()) {
                String msg = "Failed to parse recipient, cancel sending！";
                log.info(msg);
                return msg;
            }
            TCUtils.getProperty(dataManagementService, firstTarget, "object_string");
            String targetname = firstTarget.get_object_string();
            CommonsMultipartFile[] multipartFiles = null;
            if (attachments != null && attachments.length() > 0) {
                String[] split = attachments.split(";");
                multipartFiles = new CommonsMultipartFile[split.length];
                long now = System.currentTimeMillis();
                for (int i = 0; i < split.length; i++) {
                    String attachmentUid = split[i];
                    Dataset dataset = DatasetUtil.getDateSet(dataManagementService, attachmentUid);
                    File[] dataSetFiles = DatasetUtil.getDataSetFiles(dataManagementService, dataset, fileManagementUtility);
                    if (dataSetFiles == null) {
                        continue;
                    }
                    File dataSetFile = dataSetFiles[0];
                    ModelObject[] refList = dataset.get_ref_list();
                    ImanFile dsFile = (ImanFile) refList[0];
                    TCUtils.getProperty(dataManagementService, dsFile, "original_file_name");
                    String original_file_name = dsFile.get_original_file_name();
                    File temp = File.createTempFile("tcDataSet-", ".temp");
                    String filePath = temp.getParent() + File.separator+ now + File.separator + original_file_name;
                    FileUtil.rename(dataSetFile, filePath, true);
                    System.out.println(filePath);
                    multipartFiles[i] = new CommonsMultipartFile(createFileItem(new File(Access.check(filePath)), "file"));
                }
            }
            for (String s : sentToList) {
                try {
                    String[] strArr = s.split("\\|");
                    String username = strArr[0];
                    String sendTo = strArr[1];
                    String body = null;
                    if (StringUtil.isEmpty(changeDesc)) {
                        body = String.format("<html>\n" +
                                "<head></head>\n" +
                                "<body>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>Dear 『%s』，</strong></div>\n" +
                                "\t<br/>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;请登录TC账号『%s』完成流程『%s』.『%s』的签核！</strong></div>\n" +
                                "\t<br/>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>此通知由Teamcenter发送</strong></div>\n" +
                                "</html>", username, username, taskName, targetname);
                    } else {
                        body = String.format("<html>\n" +
                                "<head></head>\n" +
                                "<body>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>Dear 『%s』，</strong></div>\n" +
                                "\t<br/>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;请登录TC账号『%s』完成流程『%s』.『%s』的签核！</strong></div>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;此次变更的内容为:『%s』</strong></div>\n" +
                                "\t<br/>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>此通知由Teamcenter发送</strong></div>\n" +
                                "</html>", username, username, taskName, targetname, changeDesc);
                    }

                    String msg = "<html><head></head><body>" + body + "</body></html>";
                    Map<String, String> httpmap = new HashMap<>();
                    httpmap.put("sendTo", sendTo);
//                    httpmap.put("sendCc", "hua-sheng.yu@foxconn.com,dong.d.li@foxconn.com");
                    httpmap.put("subject", subject);
                    httpmap.put("htmlmsg", msg);
                    com.alibaba.nacos.shaded.com.google.gson.Gson gson = new com.alibaba.nacos.shaded.com.google.gson.Gson();
                    String data = gson.toJson(httpmap);
                    String result = null;
                    if (multipartFiles == null) {
                        result = tcMailClient.sendMail3Method(data);
                    }else {
                        result = tcMailClient.sendMail3Method(data, multipartFiles);
                    }
                    log.info("==>> result: " + result);
                    if (!"success".equals(result)) {
                        log.info(String.format("Email %s failed to send!", sendTo));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        } finally {
            if (tcsoaServiceFactory != null) {
                tcsoaServiceFactory.logout();
            }
        }
    }

    @Override
    public String sendMatrixEmail(String taskName, String subject, String currentName, String changeDesc, String firstTargetUid,String attachments) {
        TCSOAServiceFactory tcsoaServiceFactory = null;
        try {
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
            DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
            FileManagementUtility fileManagementUtility = tcsoaServiceFactory.getFileManagementUtility();
            ItemRevision firstTarget = (ItemRevision) TCUtils.findObjectByUid(dataManagementService, firstTargetUid);
            String project = TCUtils.getPropStr(dataManagementService, firstTarget, TCProjectConstant.PROPERTY_PROJECT_list);
            log.info("==>> project: " + project);
            List<String> sentToList = parseEmail(dataManagementService, firstTarget, fileManagementUtility, currentName);
            if (sentToList.isEmpty()) {
                String msg = "Failed to parse recipient, cancel sending！";
                log.info(msg);
                return msg;
            }
            TCUtils.getProperty(dataManagementService, firstTarget, "object_string");
            String targetname = firstTarget.get_object_string();
            Map<String, String> httpmap = new HashMap<>();
            List<MultipartFile> list = new ArrayList<>();
            if (attachments != null && attachments.length() > 0) {
                String[] split = attachments.split(";");
                long now = System.currentTimeMillis();
                for (int i = 0; i < split.length; i++) {
                    String attachmentUid = split[i];
                    Dataset dataset = DatasetUtil.getDateSet(dataManagementService, attachmentUid);
                    File[] dataSetFiles = DatasetUtil.getDataSetFiles(dataManagementService, dataset, fileManagementUtility);
                    if (dataSetFiles == null) {
                        continue;
                    }
                    File dataSetFile = dataSetFiles[0];
                    ModelObject[] refList = dataset.get_ref_list();
                    ImanFile dsFile = (ImanFile) refList[0];
                    TCUtils.getProperty(dataManagementService, dsFile, "original_file_name");
                    String original_file_name = dsFile.get_original_file_name();
                    File temp = File.createTempFile("tcDataSet-", ".temp");
                    String filePath = temp.getParent() + "\\" + now + "\\" + original_file_name;
                    FileUtil.rename(dataSetFile, filePath, true);
                    System.out.println(filePath);
                    byte[] bytes = new byte[0];
                    FileInputStream fileInputStream = null;
                    try {
                        fileInputStream = new FileInputStream(filePath);
                        bytes = FileCopyUtils.copyToByteArray(fileInputStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        try {
                            if (fileInputStream != null) {
                                fileInputStream.close();
                            }
                        }catch (IOException e){}
                    }
                    list.add(new MockMultipartFile(original_file_name, original_file_name, null,bytes));
                }
            }
            for (String s : sentToList) {
                try {
                    String[] strArr = s.split("\\|");
                    String userId = strArr[0];
                    String username = strArr[1];
                    String sendTo = strArr[2];
                    String body = null;
                    if (StringUtil.isEmpty(changeDesc)) {
                        body = String.format("<html>\n" +
                                "<head></head>\n" +
                                "<body>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>Dear 『%s』，</strong></div>\n" +
                                "\t<br/>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;请登录TC账号『%s』 完成专案文件『%s』的流程『%s』.『%s』的签核！</strong></div>\n" +
                                "\t<br/>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>此通知由Teamcenter发送</strong></div>\n" +
                                "</html>", username, userId, project, taskName, targetname);
                    } else {
                        body = String.format("<html>\n" +
                                "<head></head>\n" +
                                "<body>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>Dear 『%s』，</strong></div>\n" +
                                "\t<br/>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;请登录TC账号『%s』 完成专案文件『%s』的流程『%s』.『%s』的签核！</strong></div>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;此次变更的内容为:『%s』</strong></div>\n" +
                                "\t<br/>\n" +
                                "\t<div style=\"font-family: 宋体;  font-size:15px; \"><strong>此通知由Teamcenter发送</strong></div>\n" +
                                "</html>", username, userId, project, taskName, targetname, changeDesc);
                    }

                    String msg = "<html><head></head><body>" + body + "</body></html>";

                    httpmap.put("sendTo", sendTo);
//                    httpmap.put("sendCc", "hua-sheng.yu@foxconn.com,dong.d.li@foxconn.com");
                    httpmap.put("subject", subject);
//                    httpmap.put("subject", "【Matrix签核通知】请你登陆TC完成签核流程");
                    httpmap.put("htmlmsg", msg);
                    com.alibaba.nacos.shaded.com.google.gson.Gson gson = new com.alibaba.nacos.shaded.com.google.gson.Gson();
                    String data = gson.toJson(httpmap);
                    String result = tcMailClient.sendMail5Method(data, list);
                    log.info("==>> result: " + result);
                    if (!"success".equals(result)) {
                        log.info(String.format("Email %s failed to send!", sendTo));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        } finally {
            if (tcsoaServiceFactory != null) {
                tcsoaServiceFactory.logout();
            }
        }
    }

    public List<String> parseEmail(DataManagementService dataManagementService, ItemRevision itemRev, FileManagementUtility fileManagementUtility, String currentName) throws Exception {
        List<String> list = new ArrayList<>();
        List<ActualUserPojo> allActualUser = ActualUserUtil.getAllActualUser(dataManagementService, itemRev);
        for (ActualUserPojo actualUserPojo : allActualUser) {
            if(actualUserPojo.getProcessNode().startsWith(currentName)){
                List<String> userIds = StrSplitter.split(actualUserPojo.getTcUser(), ",", true, true);
                List<String> userNames = StrSplitter.split(actualUserPojo.getActualUserName(), ",", true, true);
                List<String> mails = StrSplitter.split(actualUserPojo.getActualUserMail(), ",", true, true);
                if(userNames.size() > 0 &&  userNames.size() == mails.size()){
                    for (int i = 0; i < userNames.size(); i++) {
                        list.add(userIds.get(0) + "|" + userNames.get(i) + "|" + mails.get(i));
                    }
                }
            }
        }
//        list.clear();

        /*TCUtils.getProperty(dataManagementService, itemRev, "IMAN_external_object_link");
        ModelObject[] modelObject = itemRev.getPropertyObject("IMAN_external_object_link").getModelObjectArrayValue();
        if (modelObject == null || modelObject.length <= 0) {
            return list;
        }
        Dataset dataset = (Dataset) modelObject[0];
        dataManagementService.refreshObjects(new ModelObject[]{dataset});
        dataManagementService.getProperties(new ModelObject[]{dataset}, new String[]{"ref_list"});
        ModelObject[] dsfiles = dataset.get_ref_list();
        for (ModelObject dsfile : dsfiles) {
            InputStreamReader inputStreamReader = null;
            FileInputStream fileInputStream = null;
            BufferedReader br = null;
            try {
                if (!(dsfile instanceof ImanFile)) {
                    continue;
                }

                ImanFile dsFile = (ImanFile) dsfile;
                dataManagementService.refreshObjects(new ModelObject[]{dsFile});
                dataManagementService.getProperties(new ModelObject[]{dsFile},
                        new String[]{"original_file_name"});
                String fileName = dsFile.get_original_file_name();
                log.info("【INFO】 fileName: " + fileName);
                // 下载数据集
                GetFileResponse responseFiles = fileManagementUtility.getFiles(new ModelObject[]{dsFile});
                File[] fileinfovec = responseFiles.getFiles();
                File file = fileinfovec[0];
                fileInputStream = new FileInputStream(file);
                inputStreamReader = new InputStreamReader(fileInputStream, "GBK");
                br = new BufferedReader(inputStreamReader);
                String line;

                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    log.info("【INFO】 line: " + line);
                    if (line.startsWith(currentName)) {
                        try {
                            String t = line.substring(line.lastIndexOf(";") + 1);
                            t = t.substring(0, t.indexOf("##"));
                            String[] ut = t.split(",");
                            String[] names = line.substring(line.indexOf("##") + 2, line.lastIndexOf("%%")).split(",");
                            String mailTmp = line.substring(line.lastIndexOf("%%") + 2);
                            String[] mails = mailTmp.split(",");
                            for (int k = 0; k < names.length; k++) {
                                if (ut.length > 0 && ut.length <= k) {
                                    list.add(ut[ut.length - 1].trim() + "|" + names[k].trim() + "|" + mails[k].trim());
                                } else {
                                    list.add(ut[k].trim() + "|" + names[k].trim() + "|" + mails[k].trim());
                                }

                            }
                        } catch (Exception e) {
                            // nothing
                        }
                    }
                }
            } finally {
                try {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                }catch (IOException e){}
                try {
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                }catch (IOException e){}

                 try {
                    if (br != null) {
                        br.close();
                    }
                 }catch (IOException e){}
            }
        }*/
        return list;
    }

    //把File转化为CommonsMultipartFile
    public FileItem createFileItem(File file, String fieldName) {
        //DiskFileItemFactory()：构造一个配置好的该类的实例
        //第一个参数threshold(阈值)：以字节为单位.在该阈值之下的item会被存储在内存中，在该阈值之上的item会被当做文件存储
        //第二个参数data repository：将在其中创建文件的目录.用于配置在创建文件项目时，当文件项目大于临界值时使用的临时文件夹，默认采用系统默认的临时文件路径
        FileItemFactory factory = new DiskFileItemFactory(16, null);
        //fieldName：表单字段的名称；第二个参数 ContentType；第三个参数isFormField；第四个：文件名
        FileItem item = factory.createItem(fieldName, "text/plain", true, file.getName());
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        FileInputStream fis = null;
        OutputStream os = null;
        try {
            fis = new FileInputStream(file);
            os = item.getOutputStream();
            while ((bytesRead = fis.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);//从buffer中得到数据进行写操作
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            }catch (IOException e){}
            try {
                if (os != null) {
                    os.close();
                }
            }catch (IOException e){}
        }
        return item;
    }
}
