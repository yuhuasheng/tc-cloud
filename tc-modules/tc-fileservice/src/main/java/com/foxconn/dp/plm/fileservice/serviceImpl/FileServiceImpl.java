package com.foxconn.dp.plm.fileservice.serviceImpl;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.dp.plm.fileservice.domain.entity.*;
import com.foxconn.dp.plm.fileservice.mapper.FileManageMapper;
import com.foxconn.dp.plm.fileservice.service.IFileService;
import com.foxconn.dp.plm.privately.FileServerPropertitesUtils;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.utils.file.FileUtil;
import com.foxconn.plm.utils.net.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 文件管理操作类
 */
@Service("fileServiceImpl")
public class FileServiceImpl implements IFileService {
    private static Log log = LogFactory.get();
    //文件存储根路径
    private final static String localFilePath = FileServerPropertitesUtils.getProperty("rootpath");

    //文件会收取路径
    private final static String bakpath = FileServerPropertitesUtils.getProperty("rootbakpath");

    //hdfs微服务地址
    private final static String hdfsUrl = FileServerPropertitesUtils.getProperty("hdfs.url");


    @Autowired(required = false)
    private FileManageMapper fileManageMapper;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Value("${server.port}")
    private String serverPort;

    /**
     * 删除文档版本
     *
     * @param docRevId    文档版本ID
     * @param fileHisPojo
     * @throws Exception
     */
    @Override
    public void deleteDocRev(Long docRevId, FileHisEntity fileHisPojo) {
        if (docRevId == null) {
            throw new BizException("文件ID为空");
        }
        if (fileHisPojo.getModified() == null || "".equalsIgnoreCase(fileHisPojo.getModified())) {
            throw new BizException("修改人为空");
        }
        //判断文件是不是已发行   已发行不让删除
        DocumentRevEntity documentRevEntity = fileManageMapper.getDocRevInfo(docRevId);
        if (documentRevEntity.getLifecyclePhase() == 0) {
            throw new BizException("存在已发行的文件，不能删除");
        }
        //删除文件
        fileManageMapper.deleteDocRev(docRevId);
        if ("01".equalsIgnoreCase(documentRevEntity.getDocRevNum())) {
            fileManageMapper.deleteDoc(documentRevEntity.getDocId());
        }

        //文档下面的文件删光了
        fileManageMapper.deleteDocRevFiles(docRevId);

        //操作描述
        String userName = "";
        try {
            String json = HttpUtil.sendGet(hdfsUrl + "/tc-hdfs/getUserInfoInSpas", "empIds=" + fileHisPojo.getModified());
            userName = JSONObject.parseObject(json).getJSONArray("data").getJSONObject(0).getString("name");
        } catch (Exception e) {
        }
        String hisDescription = "【" + fileHisPojo.getModified() + "(" + userName + ")" + "】於【" + sdf.format(new Date()) + "】删除";
        fileHisPojo.setHisDescription(hisDescription);
        fileManageMapper.addFileHistory(fileHisPojo);

        //把删除的文件放到回收区
        List<ServerListEntity> serverListEntity = fileManageMapper.getServerList();
        for (ServerListEntity s : serverListEntity) {
            new RecycleFileThread(docRevId, s).start();
        }

    }


    /**
     * 异步删除文件
     */
    private class RecycleFileThread extends Thread {
        private Long docRevId;
        private ServerListEntity serverListEntity;

        public RecycleFileThread(Long docRevId, ServerListEntity serverListEntity) {
            this.docRevId = docRevId;
            this.serverListEntity = serverListEntity;
        }

        @Override
        public void run() {
            InputStream inputStream = null;
            try {
                String netAddress = "http://" + serverListEntity.getValue() + ":" + serverPort + "/recycleFile?docRevId=" + docRevId;
                log.info("recycle url=============== " + netAddress);
                URL url = new URL(netAddress);
                URLConnection conn = url.openConnection();
                inputStream = conn.getInputStream();
                int byteread;
                byte[] buffer = new byte[1024];
                while ((byteread = inputStream.read(buffer)) != -1) {
                    System.out.println("" + byteread);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                try {
                    if (inputStream != null) {//close 前非空判斷
                        inputStream.close();//close 前要做非空判斷
                    }
                } catch (IOException e) {
                }
            }
        }
    }


    /**
     * 快速发行文档
     *
     * @param docRevId
     * @throws Exception
     */
    @Override
    public void quickReleaseDocRev(Long docRevId, FileHisEntity fileHisEntity) {
        fileManageMapper.quickReleaseDocRev(docRevId);
        //操作描述
        String userName = "";
        try {
            String json = HttpUtil.sendGet(hdfsUrl + "/tc-hdfs/getUserInfoInSpas", "empIds=" + fileHisEntity.getModified());
            userName = JSONObject.parseObject(json).getJSONArray("data").getJSONObject(0).getString("name");
        } catch (Exception e) {
        }
        String hisDescription = "【" + fileHisEntity.getModified() + "(" + userName + ")" + "】於【" + sdf.format(new Date()) + "】快速发行";
        fileHisEntity.setHisDescription(hisDescription);
        fileHisEntity.setHisAction(2);
        fileHisEntity.setFileVersionId(docRevId);
        fileManageMapper.addFileHistory(fileHisEntity);
    }


    @Override
    public void recycleFile(Long docRevId) {
        List<FileEntity> fileEntitys = fileManageMapper.getDeletedFileInfo(docRevId);
        try {
            for (FileEntity fileEntity : fileEntitys) {
                String filePath = localFilePath + fileEntity.getFilePath();
                String bakPath = bakpath + fileEntity.getFilePath();
                String bakfolder = bakpath + fileEntity.getFilePath();
                if (isWindows()) {
                    filePath = filePath.replaceAll("/", "\\\\");
                    bakPath = bakPath.replaceAll("/", "\\\\");
                    bakfolder = bakfolder.replaceAll("/", "\\\\");
                    bakfolder = bakfolder.substring(0, bakfolder.lastIndexOf("\\") + 1);
                } else {
                    filePath = filePath.replaceAll("\\\\", "/");
                    bakPath = bakPath.replaceAll("\\\\", "/");
                    bakfolder = bakfolder.replaceAll("\\\\", "/");
                    bakfolder = bakfolder.substring(0, bakfolder.lastIndexOf("/") + 1);
                }
                File srcfile = new File(FileUtil.validatePath(filePath));
                if (srcfile.exists()) {
                    File fl = new File(FileUtil.validatePath(bakfolder));
                    if (!fl.exists()) {
                        fl.mkdirs();
                    }
                    copyFile(srcfile, new File(FileUtil.validatePath(bakPath)));
                    srcfile.delete();
                }
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //复制一个文件
    public static void copyFile(File srcFile, File targetFile) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(targetFile);
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = fis.read(bytes)) != -1) {
                fos.write(bytes, 0, len);
            }
            fos.flush();
        } catch (FileNotFoundException e0) {

        } catch (Exception e) {
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
            }
        }
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
