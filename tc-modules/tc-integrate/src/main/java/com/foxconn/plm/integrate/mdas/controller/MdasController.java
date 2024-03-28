package com.foxconn.plm.integrate.mdas.controller;

import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.config.properties.MDASTypeConfig;
import com.foxconn.plm.integrate.config.properties.MinioPropertiesConfig;
import com.foxconn.plm.integrate.mdas.domain.MdasData;
import com.foxconn.plm.integrate.mdas.service.SyncMdas;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.DatasetUtil;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.strong.Dataset;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@RestController
@Scope("request")
@RequestMapping("/mdas")
public class MdasController {



    @Resource(name = "mdasMinioClient")
    private MinioClient minioClient;

    @Resource
    private MinioPropertiesConfig minioPropertiesConfig;


    @Resource
    private SyncMdas syncMdas;

    @Resource
    private MDASTypeConfig mdasTypeConfig;

    @Value("${server.port}")
    private String serverPort;


    // test
    @RequestMapping("/get")
    public String get() {
        return serverPort;
    }

    // test
    @RequestMapping("/mdasTypeConfig")
    public Map<String, String> getMdasTypeConfig() {
        return mdasTypeConfig.getTypeConfig();
    }


    @PostMapping("/sendItemDatas")
    public R sendDataToMdas(@RequestBody List<MdasData> mdasDatas) throws Exception {
        return syncMdas.sendDataToMdas(mdasDatas);
    }


    @GetMapping("/download/{uid}")
    public ResponseEntity<byte[]> fileDownload(@PathVariable String uid, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        TCSOAServiceFactory tcSOAServiceFactory=null;
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            Dataset dataset = DatasetUtil.getDateSet(dataManagementService, uid);
            FileManagementUtility fileManagementUtility = tcSOAServiceFactory.getFileManagementUtility();
            File[] files = DatasetUtil.getDataSetFiles(dataManagementService,dataset,fileManagementUtility );
            if (fileName == null || fileName.length() == 0) {
                fileName = DatasetUtil.getDataSetName(dataManagementService, dataset);
            }
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<byte[]>(Files.readAllBytes(files[0].toPath()),
                    headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            }catch(Exception e){}
        }
        return new ResponseEntity<byte[]>(headers, HttpStatus.SERVICE_UNAVAILABLE);
    }






}
