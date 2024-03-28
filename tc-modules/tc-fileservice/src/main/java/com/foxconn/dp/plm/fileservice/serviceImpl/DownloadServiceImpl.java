package com.foxconn.dp.plm.fileservice.serviceImpl;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.fileservice.domain.entity.FileEntity;
import com.foxconn.dp.plm.fileservice.domain.entity.ServerListEntity;
import com.foxconn.dp.plm.fileservice.mapper.FileManageMapper;
import com.foxconn.dp.plm.fileservice.service.IDownloadService;
import com.foxconn.dp.plm.privately.FileServerPropertitesUtils;
import com.foxconn.plm.utils.file.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;

/**
 * 文件管理操作类
 */
@Scope("prototype")
@Service("downloadServiceImpl")
public class DownloadServiceImpl implements IDownloadService {
    private static Log log = LogFactory.get();
    //文件存储根路径
    private final static String localFilePath = FileServerPropertitesUtils.getProperty("rootpath");

    @Autowired(required = false)
    private FileManageMapper fileManageMapper;


    @Value("${server.port}")
    private String serverPort;

    /**
     * 下载文件
     *
     * @param fileVersionId
     * @throws Exception
     */
    @Override
    public FileEntity downloadFile(Long fileVersionId) {
        FileEntity fileEntity = fileManageMapper.getFileInfo(fileVersionId);
        if (fileEntity == null) {
            return null;
        }
        fileEntity.setFileVersionId(fileVersionId);
        String filePath = localFilePath + fileEntity.getFilePath();
        if (isWindows()) {
            filePath.replaceAll("/", "\\\\");
        } else {
            filePath.replaceAll("\\\\", "/");
        }
        log.info("========downloadFile path:" + filePath);
        log.info("====  path2:" + FileUtil.validatePath(filePath));
        File file = new File(FileUtil.validatePath(filePath));
        //判断文件是否存在
        if (!file.exists()) {
            //从其他site 查找下载文件
            file = getFileFromOtherSite(fileEntity);
        }
        fileEntity.setOrgFile(file);
        return fileEntity;
    }


    /**
     * 从其他Site 同步文件
     *
     * @param fileVersionId
     * @throws Exception
     */
    @Override
    public FileEntity syncFileFromOtherSite(Long fileVersionId) {
        FileEntity fileEntity = fileManageMapper.getFileInfo(fileVersionId);
        if (fileEntity == null) {
            return null;
        }
        fileEntity.setFileVersionId(fileVersionId);
        String filePath = localFilePath + fileEntity.getFilePath();
        if (isWindows()) {
            filePath.replaceAll("/", "\\\\");
        } else {
            filePath.replaceAll("\\\\", "/");
        }
        log.info("========downloadFile path:" + filePath);
        File file = new File(FileUtil.validatePath(filePath));
        //判断文件是否存在
        if (!file.exists()) {
            return null;
        }
        fileEntity.setOrgFile(file);
        return fileEntity;
    }

    /**
     * 从其他Site 同步下载文件
     *
     * @param fileEntity
     * @return
     */
    private File getFileFromOtherSite(FileEntity fileEntity) {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        String filePath = null;
        try {
            //当前Site
            Integer currServerId = Integer.parseInt(FileServerPropertitesUtils.getProperty("serverid"));
            if (currServerId.intValue() == fileEntity.getServerId().intValue()) {
                return null;
            }
            List<ServerListEntity> serverListEntity = fileManageMapper.getServerList();
            String ip = null;
            for (ServerListEntity s : serverListEntity) {
                if (s.getId().intValue() == fileEntity.getServerId().intValue()) {
                    ip = s.getValue();
                    break;
                }
            }
            String netAddress = "http://" + ip + ":" + serverPort + "/syncFileFromOtherSite?fileVersionId=" + fileEntity.getFileVersionId();
            log.info("==========getFileFromOtherSite url:" + netAddress);
            URL url = new URL(netAddress);
            URLConnection conn = url.openConnection();
            inputStream = conn.getInputStream();
            filePath = localFilePath + fileEntity.getFilePath();
            if (isWindows()) {
                filePath.replaceAll("/", "\\\\");
            } else {
                filePath.replaceAll("\\\\", "/");
            }
            String fd = filePath.substring(0, filePath.lastIndexOf(File.separator));
            log.info("==========getFileFromOtherSite folder:" + fd);
            File f = new File(FileUtil.validatePath(fd));
            if (!f.exists()) {
                f.mkdirs();
            }
            log.info("==========getFileFromOtherSite filePath:" + filePath);
            fileOutputStream = new FileOutputStream(FileUtil.validatePath(filePath));
            int byteread;
            byte[] buffer = new byte[1024];
            while ((byteread = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, byteread);
                String msg = new String(buffer);
                log.info(new String(buffer));
                if (msg.contains("syncBizFileFailed")) {
                    throw new Exception("file not found");
                }
            }
            return new File(FileUtil.validatePath(filePath));
        } catch (MalformedURLException e0) {
            e0.printStackTrace();
        } catch (FileNotFoundException e1) {
            try {
                if (filePath != null) {
                    File f = new File(FileUtil.validatePath(filePath));
                    if (f.exists()) {
                        f.delete();
                    }
                }
            } catch (NullPointerException e0) {
            }
        } catch (NullPointerException e2) {
            try {
                if (filePath != null) {
                    File f = new File(FileUtil.validatePath(filePath));
                    if (f.exists()) {
                        f.delete();
                    }
                }
            } catch (NullPointerException e0) {
            }
        } catch (Exception e) {
            try {
                if (filePath != null) {
                    File f = new File(FileUtil.validatePath(filePath));
                    if (f.exists()) {
                        f.delete();
                    }
                }
            } catch (NullPointerException e0) {
            }
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
            }
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
            }
        }
        return null;
    }

    // 是不是windows
    public static boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (osName.indexOf("window") >= 0) {
            return true;
        }
        return false;
    }


}
