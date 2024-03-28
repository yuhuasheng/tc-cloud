package com.foxconn.dp.plm.fileservice.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.fileservice.domain.entity.FileEntity;
import com.foxconn.dp.plm.fileservice.service.IDownloadService;
import com.foxconn.plm.entity.exception.BizException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.net.URLEncoder;

@Api(tags = "文件下载")
@RestController
@Scope("prototype")
public class DownloadController {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    private IDownloadService downloadServiceImpl;

    @ApiOperation("下载文件")
    @GetMapping("downloadFile")
    public void downloadFile(Long fileVersionId, HttpServletResponse response) {
        ServletOutputStream outputStream = null;
        FileInputStream inputStream = null;
        try {
            log.info("begin download file fileVersionId:" + fileVersionId);
            FileEntity fileEntity = downloadServiceImpl.downloadFile(fileVersionId);
            log.info(fileEntity.getFilePath());
            String filename = fileEntity.getFileName() + "." + fileEntity.getFileType();
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
            response.addHeader("error", "file download failure");
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


    @ApiOperation("从其他Site同步文件")
    @GetMapping("syncFileFromOtherSite")
    public void syncFileFromOtherSite(Long fileVersionId, HttpServletResponse response) {
        ServletOutputStream outputStream = null;
        FileInputStream inputStream = null;
        try {
            log.info("begin synFileFromOtherSite file fileVersionId:" + fileVersionId);
            FileEntity fileEntity = downloadServiceImpl.syncFileFromOtherSite(fileVersionId);
            log.info(fileEntity.getFilePath());
            String filename = fileEntity.getFileName() + "." + fileEntity.getFileType();
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
            throw new BizException("syncBizFileFailed");
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


}
