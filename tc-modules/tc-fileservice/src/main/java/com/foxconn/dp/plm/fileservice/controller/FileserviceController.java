package com.foxconn.dp.plm.fileservice.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.fileservice.domain.entity.FileHisEntity;
import com.foxconn.dp.plm.fileservice.service.IFileService;
import com.foxconn.plm.entity.response.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "文件管理")
@RestController
public class FileserviceController {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    private IFileService fileServiceImpl;


    @ApiOperation("文件快速发行")
    @PostMapping("quickReleaseFile")
    public R<Long> quickReleaseFile(Long docRevId, String modified) {
        log.info("begin quickReleaseFile =====docRevId:" + docRevId);
        FileHisEntity fileHisEntity = new FileHisEntity();
        fileHisEntity.setModified(modified);
        fileServiceImpl.quickReleaseDocRev(docRevId, fileHisEntity);
        log.info("end quickReleaseFile =====docRevId:" + docRevId);
        return R.success(docRevId);
    }

    @ApiOperation("删除文档版本")
    @PostMapping("deleteDocRev")
    public R<String> deleteDocRev(String docRevIds, String modified) {
        String[] m = docRevIds.split(",");
        String filedIds = "";
        for (String docRevIdStr : m) {
            log.info("begin deleteFile =====docRevId:" + docRevIdStr + " modified:" + modified);
            try {
                Long docRevId = Long.parseLong(docRevIdStr);
                FileHisEntity fileHisEntity = new FileHisEntity();
                fileHisEntity.setModified(modified);
                fileHisEntity.setHisAction(1);
                fileServiceImpl.deleteDocRev(docRevId, fileHisEntity);
            } catch (Exception e) {
                log.info("deleteFile failed =====docRevId:" + docRevIdStr + " modified:" + modified);
                filedIds += docRevIdStr + ",";
            }
            log.info("end deleteFile =====docRevId:" + docRevIdStr + " modified:" + modified);
        }
        if (filedIds.endsWith(",")) {
            filedIds = filedIds.substring(0, filedIds.length() - 1);
        }
        return R.success(filedIds);
    }


    @ApiOperation("将文件放到回收区")
    @GetMapping("recycleFile")
    public R<Long> recycleFile(Long docRevId) {
        log.info("begin recycleFile =====docRevId:" + docRevId);
        fileServiceImpl.recycleFile(docRevId);
        log.info("end recycleFile =====fileVersionId:" + docRevId);
        return R.success(docRevId);
    }

}
