package com.foxconn.plm.mail.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @Classname TCMailService
 * @Description
 * @Date 2021/12/27 18:30
 * @Created by HuashengYu
 */
public interface TCMailService {

    String sendTCMailService(HashMap map, MultipartFile... files);

    String sendTCMail2Service(String host, String from, String to, String title, String contentData, String personLiable, String projectId);

    String sendTCMail3Service(HashMap map, MultipartFile... files);

    Boolean sendTCMail4Service(Map map);

    Map generateAttachments(Map map, MultipartFile... files) throws Exception ;
}
