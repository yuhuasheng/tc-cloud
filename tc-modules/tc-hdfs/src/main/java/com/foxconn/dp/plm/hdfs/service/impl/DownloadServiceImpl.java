package com.foxconn.dp.plm.hdfs.service.impl;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.hdfs.dao.xplm.FolderMapper;
import com.foxconn.dp.plm.hdfs.domain.entity.DatasetEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.FileEntity;
import com.foxconn.dp.plm.hdfs.service.DownloadService;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.file.FileUtil;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.ImanFile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * 文件管理操作类
 */
@Scope("prototype")
@Service("downloadServiceImpl")
public class DownloadServiceImpl implements DownloadService {
    private static Log log = LogFactory.get();

    @Resource
    FolderMapper folderMapper;

    /**
     * 下载文件
     *
     * @param refId
     * @throws Exception
     */
    @Override
    public FileEntity downloadFile(String refId, String site) {
        FileEntity fileEntity = new FileEntity();
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {

            if ("wh".equalsIgnoreCase(site)) {
                tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS3);
            } else if ("cq".equalsIgnoreCase(site)) {
                tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
            } else if ("lh".equalsIgnoreCase(site)) {
                tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS3);
            } else if ("tpe".equalsIgnoreCase(site)) {
                tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
            } else if ("Hsinchu".equalsIgnoreCase(site)) {
                tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
            } else {
                return null;
            }

            DataManagementService dataManagementService = tCSOAServiceFactory.getDataManagementService();

            ServiceData sdDataset = dataManagementService.loadObjects(new String[]{refId});
            Dataset dataset = (Dataset) sdDataset.getPlainObject(0);
            dataManagementService.refreshObjects(new ModelObject[]{dataset});

            dataManagementService.getProperties(new ModelObject[]{dataset}, new String[]{"ref_list"});
            ModelObject[] dsfiles = dataset.get_ref_list();
            ImanFile dsFile = null;
            for (int i = 0; i < dsfiles.length; i++) {
                if (!(dsfiles[i] instanceof ImanFile)) {
                    continue;
                }
                dsFile = (ImanFile) dsfiles[i];
                dataManagementService.refreshObjects(new ModelObject[]{dsFile});
                dataManagementService.getProperties(new ModelObject[]{dsFile},
                        new String[]{"original_file_name"});
                String fileName = dsFile.get_original_file_name();
                log.info("【INFO】 fileName: " + fileName);

                // 下载数据集
                String dirPath = System.getProperty("java.io.tmpdir");
                FileUtil.checkSecurePath(dirPath);
                log.info("【INFO】 dirPath: " + dirPath);
                FileManagementUtility fileManagementUtility = tCSOAServiceFactory.getFileManagementUtility(dirPath);

                GetFileResponse responseFiles = fileManagementUtility.getFiles(new ModelObject[]{dsFile});
                File[] fileinfovec = responseFiles.getFiles();
                File file = fileinfovec[0];
                fileEntity.setOrgFile(file);
                fileEntity.setFileName(fileName);
            }

        } catch (Exception e) {
            throw new BizException("下载文件失败");
        } finally {
            try {
                if (tCSOAServiceFactory != null) {
                    tCSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
        return fileEntity;
    }

    @Override
    public List<DatasetEntity> getFileList(Long docRevId) {
        return folderMapper.getFileList(docRevId);
    }

    @Override
    public FileEntity getFileInfo(Long fileVersionId) {
        return folderMapper.getFileInfo(fileVersionId);
    }






}
