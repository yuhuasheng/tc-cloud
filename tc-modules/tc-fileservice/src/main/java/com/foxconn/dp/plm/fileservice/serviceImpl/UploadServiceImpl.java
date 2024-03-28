package com.foxconn.dp.plm.fileservice.serviceImpl;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.dp.plm.fileservice.domain.entity.*;
import com.foxconn.dp.plm.fileservice.mapper.FileManageMapper;
import com.foxconn.dp.plm.fileservice.service.IUploadService;
import com.foxconn.dp.plm.privately.FileServerPropertitesUtils;
import com.foxconn.dp.plm.privately.PrivaFileUtis;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.utils.net.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 文件管理操作类
 */
@Scope("prototype")
@Service("uploadServiceImpl")
public class UploadServiceImpl implements IUploadService {
    private static Log log = LogFactory.get();
    //文件存储根路径
    private final static String localFilePath = FileServerPropertitesUtils.getProperty("rootpath");

    //hdfs微服务地址
    private final static String hdfsUrl = FileServerPropertitesUtils.getProperty("hdfs.url");

    @Autowired(required = false)
    private FileManageMapper fileManageMapper;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * 上传文件
     */
    @Override
    public void uploadFile(DocumentEntity documentEntity, String docName, FileEntity fileEntity, FileHisEntity fileHisEntity) {
        //检查输入
        if (fileEntity.getFile() == null) {
            throw new BizException("未选择上传的文件");
        }
        if (fileEntity.getFolderId() == null) {
            throw new BizException("文件夹ID为空");
        }
        if (docName == null || "".equalsIgnoreCase(docName.trim())) {
            throw new BizException("文檔名稱为空");
        }
        if (documentEntity.getProductLine() == null || "".equalsIgnoreCase(documentEntity.getProductLine().trim())) {
            throw new BizException("产品线为空");
        }
        if (documentEntity.getProductCode() == null || "".equalsIgnoreCase(documentEntity.getProductCode().trim())) {
            throw new BizException("产品编码为空");
        }
        if (documentEntity.getCustomer() == null || "".equalsIgnoreCase(documentEntity.getCustomer().trim())) {
            throw new BizException("客户为空");
        }
        if (documentEntity.getDocCategory() == null || "".equalsIgnoreCase(documentEntity.getDocCategory().trim())) {
            throw new BizException("文档类型为空");
        }
        if (fileHisEntity.getModified() == null || "".equalsIgnoreCase(fileHisEntity.getModified().trim())) {
            throw new BizException("修改人为空");
        }
        String userName = "";
        try {
            String json = HttpUtil.sendGet(hdfsUrl + "/tc-hdfs/getUserInfoInSpas", "empIds=" + fileHisEntity.getModified());
            userName = JSONObject.parseObject(json).getJSONArray("data").getJSONObject(0).getString("name");
        } catch (Exception e) {
        }
        documentEntity.setCreatorName(userName);

        int cnt = fileManageMapper.getDocCnt(fileEntity.getFolderId(), documentEntity.getDocName());
        if (cnt > 0) {
            throw new BizException("文件名重複");
        }
        Long fileId = fileManageMapper.getObjFileSqe();
        fileEntity.setFileId(fileId);
        fileEntity.setServerId(Integer.parseInt(FileServerPropertitesUtils.getProperty("serverid")));
        //计算文件保存路径
        String tmpFilePath = getFileStorePath(fileId);
        log.info("file path :" + tmpFilePath);
        System.out.println("file path :" + tmpFilePath);
        fileEntity.setFilePath(tmpFilePath);
        //保存文件到服务器
        saveFileToLocalFold(fileEntity);
        //保存文件
        fileManageMapper.addFile(fileEntity);

        //新建档案
        fileManageMapper.addDoc(documentEntity);
        //新建档案版本
        DocumentRevEntity documentRevEntity = new DocumentRevEntity();
        documentRevEntity.setFolderId(fileEntity.getFolderId());
        documentRevEntity.setDocId(documentEntity.getDocId());
        documentRevEntity.setDocRevName(docName);
        documentRevEntity.setLifecyclePhase(1);
        documentRevEntity.setDocRevNum(String.format("%02d", 1));
        documentRevEntity.setCreator(documentEntity.getCreator());
        documentRevEntity.setCreatorName(userName);
        documentRevEntity.setRefType(documentEntity.getDocOrigin());
        documentRevEntity.setRefId("");
        fileManageMapper.addDocRev(documentRevEntity);

        FileVersionEntity fileVersionEntity = new FileVersionEntity();
        //查找文件最新版本
        fileVersionEntity.setVersionNum(String.format("%02d", 1));
        //新建文件版本
        fileVersionEntity.setFileId(fileEntity.getFileId());
        fileVersionEntity.setDocRevId(documentRevEntity.getDocRevId());
        fileManageMapper.addFileVersion(fileVersionEntity);

        //操作描述
        String hisDescription = "【" + fileHisEntity.getModified() + "(" + userName + ")" + "】於【" + sdf.format(new Date()) + "】上传";
        fileHisEntity.setHisDescription(hisDescription);
        fileHisEntity.setHisAction(0);
        fileHisEntity.setFileVersionId(documentRevEntity.getDocRevId());
        fileManageMapper.addFileHistory(fileHisEntity);
    }

    @Override
    public void rivieseFile(Long docId, String docName, FileEntity fileEntity, FileHisEntity fileHisEntity) {
        Integer cnt = fileManageMapper.getUnReleaseCnt(docId);
        if (cnt > 0) {
            throw new BizException("存在未发行的版本，无法升版");
        }
        if (fileEntity.getFolderId() == null) {
            throw new BizException("文件夹ID为空");
        }
        if (fileEntity.getFile() == null) {
            throw new BizException("未选择上传的文件");
        }
        String userName = "";
        try {
            String json = HttpUtil.sendGet(hdfsUrl + "/tc-hdfs/getUserInfoInSpas", "empIds=" + fileHisEntity.getModified());
            userName = JSONObject.parseObject(json).getJSONArray("data").getJSONObject(0).getString("name");
        } catch (Exception e) {
        }
        Long fileId = fileManageMapper.getObjFileSqe();
        fileEntity.setFileId(fileId);
        fileEntity.setServerId(Integer.parseInt(FileServerPropertitesUtils.getProperty("serverid")));
        //计算文件保存路径
        String tmpFilePath = getFileStorePath(fileId);
        log.info("file path :" + tmpFilePath);
        System.out.println("file path :" + tmpFilePath);
        fileEntity.setFilePath(tmpFilePath);
        //保存文件到服务器
        saveFileToLocalFold(fileEntity);
        //保存文件
        fileManageMapper.addFile(fileEntity);

        //新建档案版本
        DocumentRevEntity documentRevEntity = new DocumentRevEntity();
        documentRevEntity.setFolderId(fileEntity.getFolderId());
        documentRevEntity.setDocId(docId);
        documentRevEntity.setDocRevName(docName);
        documentRevEntity.setLifecyclePhase(1);
        documentRevEntity.setCreator(documentRevEntity.getCreator() + "(" + userName + ")");
        List<String> revs = fileManageMapper.getDocRevNums(docId);
        int rev = 0;
        try {
            for (String r : revs) {
                if (Integer.parseInt(r) > rev) {
                    rev = Integer.parseInt(r);
                }
            }
        } catch (Exception e) {
        }
        rev++;

        documentRevEntity.setDocRevNum(String.format("%02d", rev));
        documentRevEntity.setCreator(fileHisEntity.getModified());
        documentRevEntity.setCreatorName(userName);
        documentRevEntity.setRefType(0);
        documentRevEntity.setRefId("");
        fileManageMapper.addDocRev(documentRevEntity);

        FileVersionEntity fileVersionEntity = new FileVersionEntity();
        fileVersionEntity.setVersionNum(String.format("%02d", 1));
        //新建文件版本
        fileVersionEntity.setFileId(fileEntity.getFileId());
        fileVersionEntity.setDocRevId(documentRevEntity.getDocRevId());
        fileManageMapper.addFileVersion(fileVersionEntity);

        //操作描述
        String hisDescription = "【" + fileHisEntity.getModified() + "(" + userName + ")" + "】於【" + sdf.format(new Date()) + "】上传";
        fileHisEntity.setHisDescription(hisDescription);
        fileHisEntity.setHisAction(0);
        fileHisEntity.setFileVersionId(fileEntity.getFileVersionId());
        fileManageMapper.addFileHistory(fileHisEntity);
    }


    /**
     * 保存文件到本地文件夹
     *
     * @param fileEntity
     * @return
     * @throws Exception
     */
    private File saveFileToLocalFold(FileEntity fileEntity) {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        try {
            MultipartFile file = fileEntity.getFile();
            String tmpFilePath = fileEntity.getFilePath();
            int BUFFER_SIZE = 1024;
            byte[] buf = new byte[BUFFER_SIZE];
            int size = 0;
            bis = new BufferedInputStream(file.getInputStream());
            File pathFile = PrivaFileUtis.getFile(localFilePath, tmpFilePath);//路徑 和 文件名 都要進行合規性檢查
            if (!pathFile.exists()) {
                pathFile.mkdirs();
            }
            fileEntity.setFileSize(file.getSize());
            String fileName = file.getOriginalFilename();
            System.out.println("file name :" + fileName);
            String extType = fileName.substring(fileName.lastIndexOf("."));
            String fname = "" + fileEntity.getFileId();
            fos = PrivaFileUtis.getFileOutputStream(localFilePath, tmpFilePath, fname, extType);
            while ((size = bis.read(buf)) != -1) {
                fos.write(buf, 0, size);
            }
            fos.flush();
            File wplFile = PrivaFileUtis.getFile(localFilePath, tmpFilePath, fname, extType);// 路徑 和 文件名 都要進行合規性檢查
            fileEntity.setFilePath(tmpFilePath + fname + extType);
            fileEntity.setFileType(extType.substring(1));
            fileEntity.setFileName(fileName.substring(0, fileName.lastIndexOf(".")));
            return wplFile;
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception e) {
            }
        }
    }


    /**
     * 生成文件上传路径
     *
     * @param fileId
     * @return
     */
    private String getFileStorePath(Long fileId) {
        String idString = String.valueOf(fileId);
        int length = idString.length();
        for (int i = 0; i < 12 - length; i++) {
            idString = "0" + idString;
        }
        String folder1 = idString.substring(0, 3);
        String folder2 = idString.substring(3, 6);
        String folder3 = idString.substring(6, 9);
        String folder4 = idString.substring(9, 12);
        String result = folder1 + File.separator + folder2 + File.separator + folder3 + File.separator + folder4 + File.separator;
        return result;
    }


}
