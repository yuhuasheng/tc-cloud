package com.foxconn.plm.tcservice.connandcable.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.connandcable.domain.CableBean;
import com.foxconn.plm.tcservice.connandcable.domain.CoCaInfo;
import com.foxconn.plm.tcservice.connandcable.service.ConnAndCableService;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author HuashengYu
 * @Date 2022/10/6 16:54
 * @Version 1.0
 */
@RestController
@RequestMapping("/connAndCable")
public class ConnAndCableController {
    private static Log log = LogFactory.get();
    @Resource
    private ConnAndCableService connAndCableService;

    @PostMapping("/importHostryConnCable")
    public R importHostryConnCable(@RequestParam MultipartFile file, @RequestParam("userId") String userId) {
        List<String> msgList = new ArrayList<>();
        try {
            msgList.add("【INFO】開始執行導入");
            Workbook wb = WorkbookFactory.create(file.getInputStream());
            Map<String, Object> retMap = connAndCableService.checkExcel(wb, file.getOriginalFilename());
            msgList = (List<String>) retMap.get("msg");
            if (retMap.get("exception")!=null) {
                boolean exceptionFlag = (boolean) retMap.get("exception");
                if (exceptionFlag) {
                    msgList.add("【ERROR】本次導入終止");
                    return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"error: runtime exception");
                }
            }
            if (retMap.get("data")!=null) {
                connAndCableService.saveData((List<CableBean>) retMap.get("data"), msgList, userId);
            }
            msgList.add("【INFO】本次导入完成");
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            msgList.add("【ERROR】" + e.getLocalizedMessage());
            msgList.add("【ERROR】本次導入終止");
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
        }
        return R.success(msgList);
    }

    @GetMapping("/downloadTemplate")
    @ApiOperation("下载Conn_Cable导入模板")
    public ResponseEntity<byte[]> downloadTemplate() {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return connAndCableService.downloadTemplate(currentTime);
    }

    /**
     * 导出数据
     * @return
     */
    @PostMapping("/exportConCaData")
    public ResponseEntity<byte[]> exportConCaData(@RequestBody JSONObject jsonData) {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        ResponseEntity<byte[]> resp = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            resp = connAndCableService.exportConCaData(tCSOAServiceFactory, jsonData);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            tCSOAServiceFactory.logout();
        }
        return resp;
    }

    /**
     * 根据料号查询 connector/cable
     * @return
     */
    @PostMapping("/getCCInfoByPN")
    public R getCCInfoByPN(@RequestBody JSONObject jsonData) {
        List<Object> ccInfoList = null;
        try {
            ccInfoList = connAndCableService.getCCInfoByPN(jsonData);
        }catch (Exception e){
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
        }
        return R.success(ccInfoList);
    }

    /**
     * 根据 hhpn 查询 connector/cable
     * @return
     */
    @GetMapping("/getCoCaInfo")
    public R getCoCaInfo(@RequestParam("hhpn") String hhpn, @RequestParam("type")String type, @RequestParam("source") String source) {
        CoCaInfo coCaInfo = null;
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            coCaInfo = connAndCableService.getCoCaInfo(tCSOAServiceFactory, hhpn, type, source);
        }catch (Exception e){
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success(coCaInfo);
    }

    /**
     * 添加 connector/cable
     * @return
     */
    @GetMapping("/addCoCaInfo")
    public R addCoCaInfo(@RequestParam("hhpn") String hhpn, @RequestParam("designPN") String designPN,
                                  @RequestParam("desc") String desc, @RequestParam("mfg") String mfg,
                                  @RequestParam("groupId") String groupId, @RequestParam("type")String type,
                                  @RequestParam("creator")String creator) {
        String result = "";
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            int affectedRow = connAndCableService.addCoCaInfo(tCSOAServiceFactory, hhpn, designPN, desc, mfg, groupId, type, creator);
            if (affectedRow == 0) {
                result = "添加失败！";
            }else {
                result = "添加成功！";
            }
        }catch (Exception e){
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success(result);
    }

    /**
     * 添加 connector
     * @return
     */
    @GetMapping("/addConnectorInfo")
    public R addConnectorInfo(@RequestParam("hhpn") String hhpn, @RequestParam("desc") String desc,
                                       @RequestParam("mfg") String mfg, @RequestParam("creator") String creator) {
        try {
            connAndCableService.addConnectorInfo(hhpn, desc, mfg, creator);
        }catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"添加失败：" + e.getMessage());
        }
        return R.success("添加成功！");
    }

    /**
     * 修改 Connector | Cable
     * @return
     */
    @PostMapping("/modifyConnector")
    public R modifyConnector(@RequestBody JSONObject jsonData) {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            connAndCableService.modifyConnector(tCSOAServiceFactory, jsonData);
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"修改失败：" + e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success("修改成功！");
    }

    /**
     * 删除 connector/cable
     * @return
     */
    @PostMapping("/delCoCaInfo")
    public R delCoCaInfo(@RequestBody JSONArray jsonData) {
        try {
            connAndCableService.delCoCaInfo(jsonData);
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"删除失败：" + e.getMessage());
        }
        return R.success("删除成功！");
    }

    /**
     * 生成Cable申请单号
     * @return
     */
    @GetMapping("/getCableRequestId")
    public R getCableRequestId() {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        String itemId = "";
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            itemId = connAndCableService.getCableRequestId(tCSOAServiceFactory,"D9_CableRequest");
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"获取失败：" + e.getMessage());
        } finally {
            tCSOAServiceFactory.logout();
        }
        return R.success((Object)itemId);
    }

    /**
     * 获取实际用户
     * @return
     */
    @GetMapping("/getActualUser")
    public R getActualUser() {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        List<String> actualUser = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            actualUser = connAndCableService.getActualUser(tCSOAServiceFactory);
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"查询失败：" + e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success(actualUser);
    }

    /**
     * 获取 Cable类型下拉值
     * @return
     */
    @GetMapping("/getCableTypeValues")
    public R getCableTypeValues() {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        List<String> cableType = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            cableType = connAndCableService.getCableTypeValues(tCSOAServiceFactory);
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"查询失败：" + e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success(cableType);
    }

    /**
     * 创建Cable申请单
     * @return
     */
    @PostMapping("/crateCableRequest")
    public R crateCableRequest(@RequestParam("id") String id, @RequestParam("type") String type,
                                        @RequestParam("reason") String reason, @RequestParam("actualUser") String actualUser,
                                        @RequestParam("connector") String connector, @RequestParam("groupId") String groupId,
                                        MultipartFile[] attachment, @RequestParam("cableList")String cableList, @RequestParam("userId")String userId) {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            connAndCableService.crateCableRequest(tCSOAServiceFactory, id, type,
                    reason, actualUser, connector, groupId, attachment, cableList, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"申请单创建失败：" + e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success("申请单创建成功！");
    }

    /**
     * 获取 itemId 版本规则的值
     * @return
     */
    @GetMapping("/getIdNRValue")
    public R getIdNRValue() {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        String[] itemIdNR = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            itemIdNR = connAndCableService.getNRValue(tCSOAServiceFactory, "D9_CABDesign", "item_id");
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"获取异常：" + e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success(itemIdNR);
    }

    /**
     * 获取 itemRev 版本规则的值
     * @return
     */
    @GetMapping("/getItemRevNRValue")
    public R getItemRevNRValue() {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        String[] itemRevNR = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            itemRevNR = connAndCableService.getNRValue(tCSOAServiceFactory, "D9_CABDesign", "item_revision_id");
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"获取异常：" + e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success(itemRevNR);
    }

    /**
     * 获取 Cable Id
     * @return
     */
    @GetMapping("/getCableId")
    public R getCableId(@RequestParam(value = "rule") String rule) {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        String generateId = "";
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            generateId = connAndCableService.generateId(tCSOAServiceFactory, rule, "D9_CABDesign");
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"获取异常：" + e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success((Object)generateId);
    }

    /**
     * 获取 Cable 版本
     * @return
     */
    @GetMapping("/getCableRev")
    public R getCableRev(@RequestParam(value = "rule") String rule) {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        String generateVersion = "";
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            generateVersion = connAndCableService.generateVersion(tCSOAServiceFactory, rule, "D9_CABDesignRevision");
        } catch (Exception e) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"获取异常：" + e.getMessage());
        }finally {
            tCSOAServiceFactory.logout();
        }
        return R.success((Object)generateVersion);
    }

}
