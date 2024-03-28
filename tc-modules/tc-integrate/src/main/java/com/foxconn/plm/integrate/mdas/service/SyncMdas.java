package com.foxconn.plm.integrate.mdas.service;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.config.properties.MinioPropertiesConfig;
import com.foxconn.plm.integrate.mdas.domain.MdasData;
import com.foxconn.plm.integrate.mdas.domain.MdasResponse;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.file.FileUtil;
import com.foxconn.plm.utils.minio.MinIoUtils;
import com.foxconn.plm.utils.net.HttpUtil;
import com.foxconn.plm.utils.tc.DataManagementUtil;
import com.foxconn.plm.utils.tc.DatasetUtil;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.strong.ImanFile;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Scope("request")
public class SyncMdas {

    private static Log log = LogFactory.get();
    /**
     * mdas web 網址
     */
    @Value("${mdas.url:http://10.141.132.184:8073/mdas}")
    private String mdasUrl;

    @Resource
    private RestTemplate restTemplate;


    @Resource(name = "mdasMinioClient")
    private MinioClient minioClient;

    @Resource
    private MinioPropertiesConfig minioPropertiesConfig;


    //@Resource
    //private MDASTypeConfig mdasTypeConfig;

    private TCSOAServiceFactory tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);

    public static final String CADFILETYPE = "prt";

    public R sendDataToMdas(List<MdasData> lists) throws Exception {
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        HttpHeaders headers = HttpUtil.getJsonHeaders();
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> typeMapping = new HashMap<>(); //mdasTypeConfig.getTypeConfig();
        log.info("tc  data ::: " + lists);
        for (int i = 0; i < lists.size(); i++) {
            MdasData data = lists.get(i);
            convertBeanForMdas(data, typeMapping);
        }
        map.put("data", lists);
        map.put("time", dataFormat.format(new Date()));
        log.info("send mdas data::: " + map);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<Map<String, Object>>(map, headers);
        ResponseEntity<MdasResponse> responseEntity = restTemplate.postForEntity(mdasUrl, requestEntity, MdasResponse.class);
        return responseHandle(responseEntity);
    }

    private MdasData convertBeanForMdas(MdasData data, Map<String, String> typeMapping) throws Exception {
        String vendor = data.getVendor();
        if (StringUtils.hasLength(vendor)) {
            vendor = vendor.substring(0, 1).toUpperCase(Locale.ENGLISH);
        }
        String basePath = data.getCategory() + "/" + data.getType() + "/";
        //String typeMappingKey = data.getCategory() + ".type." + data.getType();
        //String convertType = Optional.ofNullable(typeMapping.get(typeMappingKey)).orElse(data.getType());
        data.setPart(putFileToMdas(data.getPart(), basePath + data.getItemId()));
        data.setDoc(putFileToMdas(data.getDoc(), basePath + data.getItemId()));
        data.setPic(putFileToMdas(data.getPic(), basePath + data.getItemId()));
        data.setFile(putFileToMdas(data.getFile(), basePath + data.getItemId()));
        //data.setType(convertType);
        data.setBucketName(minioPropertiesConfig.getDtBucketName());
        data.setVendor(vendor);
        if (StringUtils.hasLength(data.getSubType())) {
            String subTypeMappingKey = data.getCategory() + ".subtype." + data.getType() + "." + data.getSubType();
            String subConvertType = Optional.ofNullable(typeMapping.get(subTypeMappingKey)).orElse(data.getSubType());
            data.setSubType(subConvertType);
        }
        data.setItemId(null); // itemid 不用抛到MDAS ，故设置为空
        return data;
    }


    public String putFileToMdas(@PathVariable String uid, String filePath) throws Exception {
        FileInputStream fileInputStream = null;
        try {
            if (StringUtils.hasLength(uid)) {
                DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
                FileManagementUtility fileManagementUtility = tcSOAServiceFactory.getFileManagementUtility();
                ImanFile imanFile = (ImanFile)DataManagementUtil.findObjectByUid(dataManagementService, uid);
                DataManagementUtil.getProperty(dataManagementService, imanFile, "original_file_name");
                File file = DatasetUtil.getImanFile(imanFile,dataManagementService,fileManagementUtility);
                String fileName = imanFile.get_original_file_name();
                FileUtil.checkFileName(fileName);
                log.info(" -->>  fileName :: " + fileName);
                FileUtil.checkSecurePath(filePath);
                filePath += getFileTypeByStr(fileName);
                fileInputStream = new FileInputStream(file);
                MinIoUtils.putObject(minioClient, minioPropertiesConfig.getDtBucketName(), filePath, fileInputStream);
                return filePath;
            }
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
            }
        }
        return "";
    }


    private String getFileTypeByStr(String fileName) {
        int index = fileName.indexOf(".");
        if (index > 0) {
            return fileName.substring(index);
        }
        return "";
    }

    R responseHandle(ResponseEntity<MdasResponse> responseEntity) {
        if (responseEntity.getStatusCodeValue() == 200) {
            MdasResponse response = responseEntity.getBody();
            if (response.getStatus() == 1) {
                return R.success("success");
            } else {
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"success");
            }
        } else {
            log.info("http error :     \n " + responseEntity.getBody());
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),responseEntity.getStatusCode().toString());
        }
    }

}
