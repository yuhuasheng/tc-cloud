package com.foxconn.dp.plm.hdfs.controller;

import cn.hutool.core.io.file.FileSystemUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.hdfs.domain.entity.DatasetEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.FileEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.UserEntity;
import com.foxconn.dp.plm.hdfs.service.DownloadService;
import com.foxconn.dp.plm.hdfs.service.UserService;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.response.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@RestController
@Scope("prototype")
public class DownloadController {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    private DownloadService downloadServiceImpl;


    @Autowired(required = false)
    private UserService userServiceImpl;

    @Value("${fsurl}")
    private String fsurl;

    @GetMapping("downloadFile")
    public void downloadFile(String refId, String site, HttpServletResponse response) {
        ServletOutputStream outputStream = null;
        FileInputStream inputStream = null;
        try {
            log.info("begin download file refId:" + refId);
            FileEntity fileEntity = downloadServiceImpl.downloadFile(refId, site);

            String filename = fileEntity.getFileName();
            // 将文件写入输入流
            inputStream = new FileInputStream(fileEntity.getOrgFile());
            response.reset();
            response.setContentType("application/octet-stream");
            response.addHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20"));
            outputStream = response.getOutputStream();
            byte[] b = new byte[1024];
            int len;
            //从输入流中读取一定数量的字节，并将其存储在缓冲区字节数组中，读到末尾返回-1
            while ((len = inputStream.read(b)) > 0) {
                outputStream.write(b, 0, len);
            }
            outputStream.flush();
        } catch (Exception ex) {
            throw new BizException("下载文件失败");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
            }
        }
    }


    @GetMapping("downloadFileFromHdfs")
    public void downloadFileFromHdfs(String fileVersionId, HttpServletResponse response) {
        ServletOutputStream outputStream = null;
        try {
            log.info("begin download file refId:" + fileVersionId);
            FileEntity fileEntity = downloadServiceImpl.getFileInfo(Long.parseLong(fileVersionId));

            String filename = fileEntity.getFileName()+"."+fileEntity.getFileType();
            String url = fsurl + "/downloadFile?fileVersionId="+fileVersionId;
            response.setContentType("application/octet-stream");
            response.addHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20"));
            outputStream= response.getOutputStream();
            HttpUtil.download(url,outputStream,true);
            outputStream.flush();
        } catch (Exception ex) {
            throw new BizException("下载文件失败");
        } finally {

            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
            }
        }
    }



    @GetMapping("getFileList")
    public R<List<DatasetEntity>> getFileList(Long docRevId) {
        log.info("begin getFileList docRevId:" + docRevId);
        List<DatasetEntity> datasetEntitys = downloadServiceImpl.getFileList(docRevId);
        return R.success(datasetEntitys);
    }

    @GetMapping("getUserInfoInSpas")
    public R<List<UserEntity>> getUserInfoInSpas(String empIds) {
        log.info("begin getUserInfoInSpas empIds:" + empIds);
        List<String> empls = new ArrayList<>();
        String[] m = empIds.split(",");
        for (String s : m) {
            empls.add(s);
        }
        List<UserEntity> userEntitys = userServiceImpl.getUserInfoInSpas(empls);
        return R.success(userEntitys);
    }
}
