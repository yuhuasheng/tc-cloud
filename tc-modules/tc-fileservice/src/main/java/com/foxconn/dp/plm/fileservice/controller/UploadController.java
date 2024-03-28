package com.foxconn.dp.plm.fileservice.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.fileservice.domain.entity.DocumentEntity;
import com.foxconn.dp.plm.fileservice.domain.entity.FileEntity;
import com.foxconn.dp.plm.fileservice.domain.entity.FileHisEntity;
import com.foxconn.dp.plm.fileservice.domain.rp.ReviseFileRp;
import com.foxconn.dp.plm.fileservice.domain.rp.UploadFileRp;
import com.foxconn.dp.plm.fileservice.service.IUploadService;
import com.foxconn.dp.plm.fileservice.serviceImpl.TCSOAClientConfigImpl;
import com.foxconn.plm.entity.response.R;
import com.teamcenter.services.strong.core.DataManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "文件上传")
@RestController
@Scope("request")
public class UploadController {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    private IUploadService uploadServiceImpl;

    @Autowired(required = false)
    private TCSOAClientConfigImpl tCSOAClientConfigImpl;

    @ApiOperation("上传文件")
    @PostMapping("uploadFile")
    public R<Long> uploadFile(UploadFileRp uploadFileRp) {
        log.info("begin upload file =====folderId:" + uploadFileRp.getFolderId() + " modified:" + uploadFileRp.getModified() + " doc name:" + uploadFileRp.getDocName());
        MultipartFile[] files = uploadFileRp.getFile();
        String docName = uploadFileRp.getDocName();
        int i = 0;
        for (MultipartFile file : files) {
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFile(file);
            fileEntity.setFolderId(uploadFileRp.getFolderId());
            DocumentEntity documentEntity = new DocumentEntity();
            documentEntity.setDocCategory(uploadFileRp.getDocCategory());
            documentEntity.setProductCode(uploadFileRp.getProductCode());
            documentEntity.setProductLine(uploadFileRp.getProductLine());
            documentEntity.setDocDescription(uploadFileRp.getDocDescription());
            documentEntity.setCustomer(uploadFileRp.getCustomer());
            documentEntity.setDocOrigin(uploadFileRp.getDocOrigin());
            documentEntity.setCreator(uploadFileRp.getModified());
            String docNameTmps = docName;
            if (i > 0) {
                docNameTmps += i;
            }
            documentEntity.setDocName(docNameTmps);
            setDocNum(documentEntity);

            FileHisEntity fileHisEntity = new FileHisEntity();
            fileHisEntity.setModified(uploadFileRp.getModified());

            uploadServiceImpl.uploadFile(documentEntity, docNameTmps, fileEntity, fileHisEntity);
            i++;
        }
        log.info("end upload file =====folderId:" + uploadFileRp.getFolderId());
        return R.success(1l);
    }


    @PostMapping("reviseFile")
    public R<Long> reviseFile(ReviseFileRp reviseFileRp) {
        log.info("begin revise file =====docId:" + reviseFileRp.getDocId() + " folderId:" + reviseFileRp.getFolderId() + " modified:" + reviseFileRp.getModified());

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFile(reviseFileRp.getFile());
        fileEntity.setFolderId(reviseFileRp.getFolderId());

        FileHisEntity fileHisEntity = new FileHisEntity();
        fileHisEntity.setModified(reviseFileRp.getModified());

        uploadServiceImpl.rivieseFile(reviseFileRp.getDocId(), reviseFileRp.getDocName(), fileEntity, fileHisEntity);
        log.info("end revise file =====docId:" + reviseFileRp.getDocId());
        return R.success(reviseFileRp.getDocId());
    }


    /**
     * 生成doc num
     *
     * @param documentEntity
     */
    private void setDocNum(DocumentEntity documentEntity) {
        DataManagementService dataManagementService = DataManagementService.getService(tCSOAClientConfigImpl.getConnection());
        com.teamcenter.services.strong.core._2014_10.DataManagement.GenerateIdInput generateIdInput = new com.teamcenter.services.strong.core._2014_10.DataManagement.GenerateIdInput();
        com.teamcenter.services.strong.core._2008_06.DataManagement.CreateInput createInput = new com.teamcenter.services.strong.core._2008_06.DataManagement.CreateInput();
        createInput.boName = "Document";
        generateIdInput.quantity = 1;
        generateIdInput.propertyName = "item_id";
        generateIdInput.createInput = createInput;
        System.out.print(generateIdInput);
        com.teamcenter.services.strong.core._2014_10.DataManagement.GenerateIdsResponse s = dataManagementService.generateIdsUsingIDGenerationRules(new com.teamcenter.services.strong.core._2014_10.DataManagement.GenerateIdInput[]{generateIdInput});
        //生成流水码
        String id = s.generateIdsOutput[0].generatedIDs[0];
        id=id.replaceAll("SD3","");
        //生成文档编号
        String docNum = "SD3" + documentEntity.getProductCode() + documentEntity.getProductLine() + documentEntity.getCustomer() + id;
        documentEntity.setDocNum(docNum);
    }

}
