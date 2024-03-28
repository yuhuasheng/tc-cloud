package com.foxconn.plm.mail.service.impl;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.foxconn.plm.mail.config.constants.ConstantsEnum;
import com.foxconn.plm.mail.config.properties.TCMailPropertiesConfig;
import com.foxconn.plm.mail.service.TCMailService;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.file.FileUtil;
import com.foxconn.plm.utils.string.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.internet.*;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;

/**
 * @Classname TCMailServiceImpl
 * @Description
 * @Date 2021/12/27 18:31
 * @Created by HuashengYu
 */
@Service
public class TCMailServiceImpl implements TCMailService {

    private static Log log = LogFactory.get();
    private String body = ""; //邮件正文路径
    private String attachments = ""; //邮件附件路径

    @Autowired
    private TCMailPropertiesConfig tcMailPropertiesConfig;

    @Override
    public String sendTCMailService(HashMap map, MultipartFile... files) {
        try {
            String to = map.get("to") == null ? "" : map.get("to").toString().trim();
            log.info("【INFO】 to: " + to);
            String user = map.get("user") == null ? "" : map.get("user").toString().trim();
            log.info("【INFO】 user: " + user);
            String server = map.get("server") == null ? "" : map.get("server").toString().trim();
            log.info("【INFO】 server: " + server);
            String subject = map.get("subject") == null ? "" : URLDecoder.decode(map.get("subject").toString().trim(), "UTF-8");
            log.info("【INFO】 subject: " + subject);
            //获取附件列表
            Map<String, String> attachmentMap = getAttachments(files);
            if (CollectUtil.isEmpty(attachmentMap)) {
                throw new Exception("【ERROR】 获取附件列表文件失败...");
            }
            attachmentMap.forEach((key, value) -> {
                if (key.contains("_body")) {
                    body = value;
                } else if (key.contains("attachment_list")) {
                    attachments = value;
                }
            });
            String[] split = to.split("&&");
            String toUser = "";
            for (String str : split) {
                toUser += "-to=" + str + " ";
            }
            String command = "";
            if (StringUtil.isEmpty(attachments)) {
                command = ConstantsEnum.TC_MAIL_EXE_PATH.value() + " " + toUser + "-user=" + user + " " + "-server=" + server + " "
                        + "-subject=" + subject + " " + "-body=" + body;
            } else {
                command = ConstantsEnum.TC_MAIL_EXE_PATH.value() + " " + toUser + "-user=" + user + " " + "-server=" + server + " "
                        + "-subject=" + subject + " " + "-body=" + body + " " + "-attachments=" + attachments;
            }
            log.info("【INFO】 toUser: " + toUser);
            log.info("【INFO】 command: " + command);

            System.out.println("【INFO】 command: " + command);

            System.out.println("【INFO】 开始发送邮件...");
            log.info("【INFO】 开始发送邮件...");

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            log.info("【INFO】 发送邮件结束...");
            System.out.println("【INFO】 发送邮件结束...");
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return "failure";
    }

    /**
     * 获取附件列表
     *
     * @param files 上传的文件
     * @return
     */
    private Map<String, String> getAttachments(MultipartFile... files) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        // 查找保存保存邮件附件的文件夹
        String filePath = FileUtil.getFilePath(ConstantsEnum.TCMAILFOLENAME.value());
        log.info("【INFO】 filePath: " + filePath);
        // 先清空文件夹
        FileUtil.deletefile(filePath);
        for (MultipartFile multipartFile : files) {
            if (multipartFile == null || multipartFile.isEmpty()) {
                log.warn("have empty upload file,you need check is right?");
                continue;
            }
            String fileName = URLDecoder.decode(multipartFile.getOriginalFilename(), "UTF-8");
            log.info("【INFO】" + fileName);

            if (filePath.endsWith("\\")) {
                filePath = filePath + fileName;
            } else {
                filePath = filePath + File.separator + fileName;
            }
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            OutputStream out = null;
            try {
                //获取文件流，以文件流的方式输出到新文件
                out = new FileOutputStream(file);
                byte[] ss = multipartFile.getBytes();
                for (int i = 0; i < ss.length; i++) {
                    out.write(ss[i]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            map.put(file.getName(), file.getAbsolutePath());
        }
        return map;
    }


    @Override
    public String sendTCMail2Service(String host, String from, String to, String title, String contentData, String personLiable, String projectId) {
        return null;
      /*  String state = "failed";
        try {
            StringBuilder content = new StringBuilder("<html><head></head><body>");
            content.append("<p><h3>Dear " + personLiable + "：</h3></p>");
            content.append("<p><span style=\"text-indent:2em;\"><h3>请完成专案『" + projectId + "』以下设计任务.</h3></span></p>");
            //border="5"
            content.append("<table style=\"border:solid 1px #E8F2F9;font-size:18px;\">");
            content.append("<tr style=\"background-color: #428BCA; color:#ffffff\">");
            content.append("<th>編號</th><th>共用賬號</th><th>零組件名稱</th></tr>");
            String[] shareAccountAndItemNames = contentData.split(",");
            for (int i = 0; i < shareAccountAndItemNames.length; i++) {
                String shareAccountAndItemName = shareAccountAndItemNames[i];
                String[] saain = shareAccountAndItemName.split("-");
                content.append("<tr>");
                content.append("<td align=\"center\">" + (i + 1) + "</td>"); //第一列
                content.append("<td align=\"center\">" + saain[0] + "</td>"); //第一列
                content.append("<td align=\"center\">" + saain[1] + "</td>"); //第一列
                content.append("</tr>");
            }
            content.append("</table>");
            content.append("<h3>谢谢！</h3>");
            content.append("</body></html>");

            log.info("PLM Send message begin");
            log.info("mail title:" + title);
            log.info("mail host:" + host);
            log.info("mail to: " + to);
            log.info("mail content:" + content);

            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.host", host);

            Session mailSession = Session.getDefaultInstance(props,new Authenticator(){
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return super.getPasswordAuthentication();
                }
            });
            mailSession.setDebug(true);

            Transport transport = mailSession.getTransport();
            MimeMessage message = new MimeMessage(mailSession);
            message.setSubject(title);

            message.setFrom(new InternetAddress(from));
            InternetAddress[] toaddress = getInternetAddress(new String[]{to});
            message.addRecipients(Message.RecipientType.TO, toaddress);

            MimeMultipart multipart = new MimeMultipart("related");
            try {
                BodyPart mbp = new MimeBodyPart();
                mbp.setContent(content.toString(), "text/html;charset=utf-8");
                multipart.addBodyPart(mbp);
            } catch (Exception e) {
            }

            message.setContent(multipart);
            message.saveChanges();
            transport.connect();

            transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
            if (message.getRecipients(Message.RecipientType.CC) != null) {
                transport.sendMessage(message, message.getRecipients(Message.RecipientType.CC));
            }
            transport.close();
            log.info("PLM Send message successful");
            state = "successful";
        } catch (Exception e) {
            log.info("PLM Send message failed!");
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        return state;

       */
    }

    /**
     * 发送邮件包含带附件的
     *
     * @param map
     * @param files
     * @return
     */
    @Override
    public String sendTCMail3Service(HashMap map, MultipartFile... files) {
//        log.info("==>> sendTCMail3Service: " + map);
//        String flag = "failure";
//        try {
//            // 收件人的邮箱（必填）（多个用英文逗号隔开）
//            String sendTo = map.get("sendTo") == null ? "" : map.get("sendTo").toString().trim();
//            log.info("==>> sendTo: " + sendTo);
//            // 抄送人的邮箱（多个用英文逗号隔开）
//            String sendCc = map.get("sendCc") == null ? "" : map.get("sendCc").toString().trim();
//            log.info("==>> sendCc: " + sendCc);
//            // 密送人的邮箱（多个用英文逗号隔开）
//            String sendBcc = map.get("sendBcc") == null ? "" : map.get("sendBcc").toString().trim();
//            log.info("==>> sendBcc: " + sendBcc);
//            // 邮件主题（必填）
//            String subject = map.get("subject") == null ? "" : map.get("subject").toString().trim();
//            log.info("==>> subject: " + subject);
//            // 邮件内容（必填）
//            String htmlmsg = map.get("htmlmsg") == null ? "" : map.get("htmlmsg").toString().trim();
//            log.info("==>> htmlmsg: " + htmlmsg);
//
//            // 发件人的名称（必填），相当于称呼，通常显示在你的发件人栏的发件人邮箱地址前
//            String fromName = map.get("fromName") == null ? "" : map.get("fromName").toString().trim();
//            log.info("==>> fromName: " + fromName);
//            // 获取附件列表
//            List attachment = getAttachmentsNew(files);
//
//            // 邮箱账号
//            String myEmailAccount = map.get("myEmailAccount") == null ? "" : map.get("myEmailAccount").toString().trim();
//            // 邮箱密码
//            String myEmailPassword = map.get("myEmailPassword") == null ? "" : map.get("myEmailPassword").toString().trim();
//
//            if ("".equals(myEmailAccount)) {
////                myEmailAccount = "cmm-it-plm@mail.foxconn.com";
//                myEmailAccount = tcMailPropertiesConfig.getAdministratorEmail();
//            }
//
//
//            log.info("************ Mail Send Message start ************");
////            String myEmailSMTPHost = "10.134.28.97"; // 邮箱服务器ip
//            String myEmailSMTPHost = tcMailPropertiesConfig.getHost(); // 邮箱服务器ip
////            String myEmailSMTPHost = "250-smtp1.foxconn.com";
////            String port = "25";
//            String port = tcMailPropertiesConfig.getPort(); // 邮箱服务器端口号
////            String port = "456";
//            String protocol = "smtp";
//
//            //创建参数配置, 用于连接邮件服务器的参数配置
//            System.setProperty("mail.mime.splitlongparameters","false");
//            Properties props = new Properties();
//            props.setProperty("mail.smtp.auth", "true");
//            props.setProperty("mail.transport.protocol", protocol);
//            props.setProperty("mail.smtp.host", myEmailSMTPHost);
//            props.setProperty("mail.smtp.port", port);
//
//            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//            props.setProperty("mail.smtp.socketFactory.fallback", "false");
//            props.setProperty("mail.smtp.socketFactory.port", port);
//
//            //根据配置创建会话对象, 用于和邮件服务器交互
//            Session session = Session.getDefaultInstance(props);
//            session.setDebug(true); // 设置为debug模式, 可以查看详细的发送 log
//
//            //创建邮件
//            MimeMessage message = new MimeMessage(session);
//            //邮件主题
//            message.setSubject(subject, "UTF-8");
//            //发件人
//            message.setFrom(new InternetAddress(myEmailAccount, fromName, "UTF-8"));
//            //收件人
//            if (!"".equals(sendTo)) {
//                String[] sendToArr = sendTo.split(",");
//                InternetAddress[] sendToAdd = new InternetAddress[sendToArr.length];
//                for (int i = 0; i < sendToArr.length; i++) {
//                    sendToAdd[i] = new InternetAddress(sendToArr[i]);
//                }
//                message.setRecipients(MimeMessage.RecipientType.TO, sendToAdd);
//            }
//            // 抄送人
//            if (!"".equals(sendCc)) {
//                String[] sendCcArr = sendCc.split(",");
//                InternetAddress[] sendCcAdd = new InternetAddress[sendCcArr.length];
//                for (int i = 0; i < sendCcArr.length; i++) {
//                    sendCcAdd[i] = new InternetAddress(sendCcArr[i]);
//                }
//                message.setRecipients(MimeMessage.RecipientType.CC, sendCcAdd);
//            }
//            // 密送人
//            if (!"".equals(sendBcc)) {
//                String[] sendBccArr = sendBcc.split(",");
//                InternetAddress[] sendBccAdd = new InternetAddress[sendBccArr.length];
//                for (int i = 0; i < sendBccArr.length; i++) {
//                    sendBccAdd[i] = new InternetAddress(sendBccArr[i]);
//                }
//                message.setRecipients(MimeMessage.RecipientType.BCC, sendBccAdd);
//            }
//            // 邮件正文（可以使用html标签）
//            MimeBodyPart msgBody = new MimeBodyPart();
//            msgBody.setContent(htmlmsg, "text/html;charset=UTF-8");
//            // 邮件内容
//            Multipart multipart = new MimeMultipart();
//            multipart.addBodyPart(msgBody);
//            //邮件附件
//            if (attachment != null) {
//                int len = attachment.size();
//                for (int i = 0; i < len; i++) {
//                    HashMap obj = (HashMap) attachment.get(i);
//                    String fileName = obj.get("fileName").toString().trim();
//                    String filePath = obj.get("filePath").toString().trim();
//                    FileDataSource fds = new FileDataSource(filePath);
//                    MimeBodyPart attBody = new MimeBodyPart();
//                    attBody.setDataHandler(new DataHandler(fds));
//                    attBody.setFileName(MimeUtility.encodeText(fileName, "utf-8","B"));
//                    multipart.addBodyPart(attBody);
//                }
//            }
//            //设置邮件内容
//            message.setContent(multipart);
//            //设置发件时间
//            message.setSentDate(new Date());
//            //保存设置
//            message.saveChanges();
//
//            //根据 Session 获取邮件传输对象
//            Transport transport = session.getTransport();
//            transport.connect(myEmailAccount, myEmailPassword);
//            transport.sendMessage(message, message.getAllRecipients());
//            transport.close();
//            log.info("************ Mail Send Message success ************");
//            flag = "success";
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.info("************ Mail Send Message end ************");
//            log.error("==>> sendTCMail3Service: " + e.getMessage());
//            flag = "failure";
//        }
//        log.info("==>> sendTCMail3Service: " + flag);
//        return flag;
        return null;
    }

    /**
     * 获取附件列表
     *
     * @param files 上传的文件
     * @return
     */
    private List getAttachmentsNew(MultipartFile... files) throws Exception {
        List attachment = new ArrayList();
        if (CollectUtil.isEmpty(files)) {
            return null;
        }
        // 查找保存保存邮件附件的文件夹
        String filePath = FileUtil.getFilePath(ConstantsEnum.TCMAILATTACHMENTFOLD.value());
        log.info("【INFO】 filePath: " + filePath);
        for (MultipartFile multipartFile : files) {
            if (multipartFile == null || multipartFile.isEmpty()) {
                log.warn("have empty upload file,you need check is right?");
                continue;
            }
            String fileName = URLDecoder.decode(multipartFile.getOriginalFilename(), "UTF-8");
            log.info("【INFO】" + fileName);

            if (filePath.endsWith(File.separator)) {
                filePath = filePath + fileName;
            } else {
                filePath = filePath + File.separator + fileName;
            }
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            OutputStream out = null;
            try {
                //获取文件流，以文件流的方式输出到新文件
                out = new FileOutputStream(file);
                byte[] ss = multipartFile.getBytes();
                for (int i = 0; i < ss.length; i++) {
                    out.write(ss[i]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            HashMap obj = new HashMap();
            obj.put("fileName", file.getName());
            obj.put("filePath", file.getAbsolutePath());
            attachment.add(obj);
        }
        return attachment;
    }


    /**
     * 生成附件到本地
     *
     * @param map
     * @param files
     * @throws Exception
     */
    public Map generateAttachments(Map map, MultipartFile... files) throws Exception {
        if (files == null) {
            return map;
        }

        for (MultipartFile multipartFile : files) {
            // 查找保存保存邮件附件的文件夹
            String filePath = FileUtil.getFilePath(ConstantsEnum.TCMAILATTACHMENTFOLD.value());
            log.info("【INFO】 filePath: " + filePath);
            if (multipartFile == null || multipartFile.isEmpty()) {
                log.warn("have empty upload file,you need check is right?");
                continue;
            }
            String fileName = URLDecoder.decode(multipartFile.getOriginalFilename(), "UTF-8");
            log.info("【INFO】" + fileName);

            if (filePath.endsWith(File.separator)) {
                filePath = filePath + fileName;
            } else {
                filePath = filePath + File.separator + fileName;
            }
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            OutputStream out = null;
            try {
                //获取文件流，以文件流的方式输出到新文件
                out = new FileOutputStream(file);
                byte[] ss = multipartFile.getBytes();
                for (int i = 0; i < ss.length; i++) {
                    out.write(ss[i]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            map.put(file.getName(), file.getName());
            map.put(file.getAbsolutePath(), file.getAbsolutePath());
        }
        return map;
    }


    /**
     * 获取本地文件集合
     * @param map
     * @return
     */
    private List<File> getLocalAttachments(Map map) {
        List<File> attachment = new ArrayList();
        map.forEach((key, value) -> {
            File file = new File((String) key);
            if (file.isFile() && file.exists()) {
                attachment.add(file);
            }
        });
        return attachment;
    }


    private static InternetAddress[] getInternetAddress(String[] mailAddresses) throws AddressException {
        if (null == mailAddresses || mailAddresses.length == 0) {
            return null;
        }
        List<InternetAddress> addressList = new ArrayList<>();
        for (String mailAddress : mailAddresses) {
            if (mailAddress == null || "".equals(mailAddress) || !mailAddress.contains("@")) {
                continue;
            }
            //過濾掉有問題的郵箱地址
            try {
                addressList.add(new InternetAddress(mailAddress));
            } catch (Exception e) {
                log.error("address error :" + mailAddress, e);
            }
        }
        if (addressList.size() == 0) {
            return null;
        }
        InternetAddress[] addresses = new InternetAddress[addressList.size()];
        return addressList.toArray(addresses);
    }

    public Boolean sendTCMail4Service(Map map) {
        log.info("==>> sendTCMail4Service: " + map);
        Boolean flag = true;
        try {
            // 收件人的邮箱（必填）（多个用英文逗号隔开）
            String sendTo = map.get("sendTo") == null ? "" : map.get("sendTo").toString().trim();
            log.info("==>> sendTo: " + sendTo);
            // 抄送人的邮箱（多个用英文逗号隔开）
            String sendCc = map.get("sendCc") == null ? "" : map.get("sendCc").toString().trim();
            log.info("==>> sendCc: " + sendCc);
            // 密送人的邮箱（多个用英文逗号隔开）
            String sendBcc = map.get("sendBcc") == null ? "" : map.get("sendBcc").toString().trim();
            log.info("==>> sendBcc: " + sendBcc);
            // 邮件主题（必填）
            String subject = map.get("subject") == null ? "" : map.get("subject").toString().trim();
            log.info("==>> subject: " + subject);
            // 邮件内容（必填）
            String htmlmsg = map.get("htmlmsg") == null ? "" : map.get("htmlmsg").toString().trim();
            log.info("==>> htmlmsg: " + htmlmsg);

            Boolean htmlFlag = null;
            if (htmlmsg.contains("html")) {
                htmlFlag = true;
            } else {
                htmlFlag = false;
            }
            // 发件人的名称（必填），相当于称呼，通常显示在你的发件人栏的发件人邮箱地址前
            String fromName = map.get("fromName") == null ? "" : map.get("fromName").toString().trim();
            log.info("==>> fromName: " + fromName);
            // 获取附件列表
            List<File> attachment = getLocalAttachments(map);

            // 邮箱账号
            String myEmailAccount = map.get("myEmailAccount") == null ? "" : map.get("myEmailAccount").toString().trim();
            // 邮箱密码
            String myEmailPassword = map.get("myEmailPassword") == null ? "" : map.get("myEmailPassword").toString().trim();

            if ("".equals(myEmailAccount)) {
                myEmailAccount = tcMailPropertiesConfig.getAdministratorEmail();
            }

            System.setProperty("mail.mime.splitlongparameters", "false");//设置系统值 ---处理文件名乱码

            // 初始化发件人信息
            MailAccount account = new MailAccount();
//            account.setDebug(true); // 设置为debug模式, 可以查看详细的发送 log
            account.setHost(tcMailPropertiesConfig.getHost());
            account.setPort(Integer.parseInt(tcMailPropertiesConfig.getPort()));
            account.setFrom(myEmailAccount);
            Set<String> to = null;
            Set<String> cc = null;
            Set<String> bcc = null;
            if (!"".equals(sendTo)) {
                to = ignoreMail(sendTo);
            }
            // 抄送人
            if (!"".equals(sendCc)) {
                cc = ignoreMail(sendCc);
            }

            // 密送人
            if (!"".equals(sendBcc)) {
                bcc = ignoreMail(sendBcc);
            }

            MailUtil.send(account, to, cc, bcc, subject, htmlmsg, htmlFlag, CollUtil.isEmpty(attachment) ? null : attachment.toArray(new File[0]));
            log.info("************ Mail Send Message success ************");
        } catch (Exception e) {
            e.printStackTrace();
            log.info("************ Mail Send Message end ************");
            log.error("==>> sendTCMail4Service: " + e.getMessage());
            flag = false;
        }
        log.info("==>> sendTCMail4Service: " + flag);
        return flag;
    }

    private List<File> getAttachmentsTest(MultipartFile... files) throws UnsupportedEncodingException {
        List attachment = new ArrayList();
        if (CollectUtil.isEmpty(files)) {
            return null;
        }
        // 查找保存保存邮件附件的文件夹
        String filePath = FileUtil.getFilePath(ConstantsEnum.TCMAILATTACHMENTFOLD.value());
        log.info("【INFO】 filePath: " + filePath);
        for (MultipartFile multipartFile : files) {
            if (multipartFile == null || multipartFile.isEmpty()) {
                log.warn("have empty upload file,you need check is right?");
                continue;
            }
            String fileName = URLDecoder.decode(multipartFile.getOriginalFilename(), "UTF-8");
            log.info("【INFO】" + fileName);

            if (filePath.endsWith(File.separator)) {
                filePath = filePath + fileName;
            } else {
                filePath = filePath + File.separator + fileName;
            }
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            OutputStream out = null;
            try {
                //获取文件流，以文件流的方式输出到新文件
                out = new FileOutputStream(file);
                byte[] ss = multipartFile.getBytes();
                for (int i = 0; i < ss.length; i++) {
                    out.write(ss[i]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            attachment.add(file);
        }
        return attachment;
    }


    private Set<String> ignoreMail(String str) {
        String[] split = str.split(",");
        if (CollectUtil.isEmpty(split)) {
            return null;
        }
        Set<String> set = new HashSet<>(split.length);
        // 校驗收件人郵箱是否正確，忽略錯誤的郵箱
        for (String mail : split) {
            if (StringUtil.isEmpty(mail) || !mail.contains("@")) {
                continue;
            }
            try {
                new InternetAddress(mail);
            } catch (Exception e) {
                log.error("address error :" + mail, e);
                continue;
            }
            set.add(mail);
        }
        return set;
    }
}
