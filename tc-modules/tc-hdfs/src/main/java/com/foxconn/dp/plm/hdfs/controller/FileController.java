package com.foxconn.dp.plm.hdfs.controller;

import com.foxconn.dp.plm.hdfs.domain.entity.FolderEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.ItemRevEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.*;
import com.foxconn.dp.plm.hdfs.service.FolderService;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.utils.string.StringUtil;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Api(tags = "文件和文件夹")
@RestController("file")
public class FileController {

    @Resource
    FolderService folderService;

    @ApiOperation("獲取专案子文件夾")
    @PostMapping(value = "/getProjectFolder")
    public RList<FolderEntity> getProjectFolder(@RequestBody FolderListRp rp) {
        PageInfo<FolderEntity> pageInfo = folderService.getProjectFolder(rp);
        return RList.ok(pageInfo.getList(), pageInfo.getTotal());
    }

    @ApiOperation("获取子文件夾")
    @PostMapping(value = "/expendFolderByParentId")
    public RList<FolderEntity> expendFolderByParentId(@RequestBody SubFolderListRp rp, HttpServletRequest request) {
        String dept = rp.getDept();
        if (StringUtil.isEmpty(dept)) {
            rp.setDept(request.getHeader("dept"));
        }
        String empId = request.getHeader("empId");
        rp.setEmpId(empId);
        List<FolderEntity> list = folderService.getSubFolder(rp);
        return RList.ok(list, list.size());
    }


    @ApiOperation("獲取文件夾下面的Item對象")
    @PostMapping(value = "/getItemByFolderId")
    public RList<ItemRevEntity> getItemByFolderId(@RequestBody ItemListRp rp) {
        PageInfo<ItemRevEntity> pageInfo = folderService.getItem(rp);
        return RList.ok(pageInfo.getList(), pageInfo.getTotal());
    }

    @ApiOperation("獲取文档下所有文件版本")
    @PostMapping(value = "/getAllItemRevisionByDocId")
    public RList<ItemRevEntity> getAllItemRevisionByDocId(@RequestBody ItemRevisionListRp rp) {
        PageInfo<ItemRevEntity> pageInfo = folderService.getAllItemRevisionByDocId(rp);
        return RList.ok(pageInfo.getList(), pageInfo.getTotal());
    }

    @ApiOperation(value = "创建文件夹")
    @PostMapping(value = "/createFolder")
    public R<Long> createFolder(@RequestBody @Validated CreateFolderRp rp) {
        return R.success(folderService.createFolder(rp));
    }

    @ApiOperation("删除文件夹")
    @PostMapping(value = "/delFolder")
    public R<String> delFolder(@RequestBody DelRp rp) {
        folderService.delFolder(rp);
        return R.success();
    }

    @ApiOperation("修改文件夹")
    @PostMapping(value = "/modifyFolder")
    public R<String> modifyFolder(@RequestBody ModifyFolderRp rp) {
        folderService.modifyFolder(rp);
        return R.success();
    }

    @ApiOperation("修改文档名称")
    @PostMapping(value = "/modifyDocName")
    public R<String> modifyDocName(@RequestBody Map<String, String> data) {
        String docNum = data.get("docNum");
        String docRev = data.get("docRev");
        String docName = data.get("docName");
        String folderId = data.get("folderId");
        folderService.modifyDocName(docNum, docRev, docName, Integer.parseInt(folderId));
        return R.success();
    }

}
