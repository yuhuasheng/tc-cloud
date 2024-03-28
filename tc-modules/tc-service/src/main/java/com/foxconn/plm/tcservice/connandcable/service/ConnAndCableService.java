package com.foxconn.plm.tcservice.connandcable.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.connandcable.domain.CableBean;
import com.foxconn.plm.tcservice.connandcable.domain.CoCaInfo;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/10/7 8:48
 * @Version 1.0
 */
public interface ConnAndCableService {

    Map<String, Object> checkExcel(Workbook wb, String fileName) throws Exception;

    void saveData(List<CableBean> list, List<String> msgList, String userId) throws Exception;

    ResponseEntity<byte[]> downloadTemplate(String currentTime);

    ResponseEntity<byte[]> exportConCaData(TCSOAServiceFactory tCSOAServiceFactory, JSONObject jsonData) throws Exception;

    List<Object> getCCInfoByPN(JSONObject jsonData) throws Exception;

    CoCaInfo getCoCaInfo(TCSOAServiceFactory tCSOAServiceFactory, String hhpn, String type, String source) throws Exception;

    int addCoCaInfo(TCSOAServiceFactory tCSOAServiceFactory, String hhpn, String designPN, String desc,
                    String mfg, String groupId, String type, String creator) throws Exception;

    void addConnectorInfo(String hhpn, String desc, String mfg, String creator) throws Exception;

    void modifyConnector(TCSOAServiceFactory tCSOAServiceFactory, JSONObject jsonData) throws Exception;

    void delCoCaInfo(JSONArray jsonData) throws Exception;

    String getCableRequestId(TCSOAServiceFactory tCSOAServiceFactory, String itemType) throws Exception;

    List<String> getActualUser(TCSOAServiceFactory tCSOAServiceFactory) throws Exception;

    List<String> getCableTypeValues(TCSOAServiceFactory tCSOAServiceFactory) throws Exception;

    void crateCableRequest(TCSOAServiceFactory tCSOAServiceFactory, String id, String type, String reason, String actualUser,
                           String connector, String groupId, MultipartFile[] attachment, String cableList, String userId) throws Exception;

    String[] getNRValue(TCSOAServiceFactory tCSOAServiceFactory, String itemType, String propName) throws Exception;;

    String generateId(TCSOAServiceFactory tCSOAServiceFactory, String rule, String itemType) throws Exception;;

    String generateVersion(TCSOAServiceFactory tCSOAServiceFactory, String rule, String itemRevType) throws Exception;;
}
