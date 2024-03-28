package com.foxconn.plm.integrate.sap.maker.service;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.param.MakerPNRp;
import com.foxconn.plm.integrate.sap.customPN.mapper.CustomPNMapper;
import com.foxconn.plm.integrate.sap.customPN.service.PostMakerInforToSAP;
import com.foxconn.plm.integrate.sap.maker.domain.MakerInfoEntity;
import com.foxconn.plm.integrate.sap.maker.domain.MakerInfor;
import com.foxconn.plm.integrate.sap.maker.domain.rp.SearchMakerRp;
import com.foxconn.plm.integrate.sap.maker.mapper.MakerMapper;
import com.foxconn.plm.integrate.sap.rfc.domain.rp.PNSupplierInfo;
import com.foxconn.plm.integrate.sap.rfc.mapper.SAPSupplierMapper;
import com.foxconn.plm.integrate.sap.utils.DestinationUtils;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.DataManagementUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoTable;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Service
public class MakerService {

    private static Log log = LogFactory.get();
    @Autowired(required = false)
    private MakerMapper makerMapper;

    @Autowired(required = false)
    private SAPSupplierMapper sapSupplierMapper;


    @Autowired(required = false)
    private CustomPNMapper customPNMapper;

    /**
     * 查询供应商信息
     *
     * @throws Exception
     */
    public List<MakerInfoEntity> searchMakerInfo(SearchMakerRp searchMakerRp) {
        String makerCode = searchMakerRp.getMakerCode();
        if (makerCode == null || "".equalsIgnoreCase(makerCode.trim())) {
            makerCode = null;
        } else {
            makerCode = makerCode.toUpperCase(Locale.ENGLISH);
        }
        searchMakerRp.setMakerCode(makerCode);

        String makerComcact = searchMakerRp.getMakerContact();
        if (makerComcact == null || "".equalsIgnoreCase(makerComcact.trim())) {
            makerComcact = null;
        } else {
            makerComcact = makerComcact.toUpperCase(Locale.ENGLISH);
        }
        searchMakerRp.setMakerContact(makerComcact);

        String makerName = searchMakerRp.getMakerName();
        if (makerName == null || "".equalsIgnoreCase(makerName)) {
            makerName = null;
        } else {
            makerName = makerName.toUpperCase(Locale.ENGLISH);
        }
        searchMakerRp.setMakerName(makerName);
        return makerMapper.searchMakerInfo(searchMakerRp);

    }


    public List<MakerInfor> postMakerPN(JCoDestination destination, JCoDestination destination888, JCoDestination destination868, List<MakerPNRp> makerPNRps) throws Exception {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        List<MakerInfor> makerInfors = new ArrayList<>();
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            DataManagementService dataManagementService = tCSOAServiceFactory.getDataManagementService();
            for (MakerPNRp makerPNRp : makerPNRps) {
                JCoDestination jco = DestinationUtils.getJCoDestination(makerPNRp.getPlant(), destination, destination888, destination868);
                ServiceData sdDataset = dataManagementService.loadObjects(new String[]{makerPNRp.getPuid()});
                ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
                post(dataManagementService,irv,makerPNRp.getPlant(),jco);
            }
        } finally {
            try {
                if (tCSOAServiceFactory != null) {
                    tCSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
        return makerInfors;
    }


    private String post(DataManagementService dataManagementService,ItemRevision irv, String plant,  JCoDestination destination) {
        try {

            DataManagementUtil.getProperty(dataManagementService, irv, "d9_SupplierZF");
            String mfgName = irv.getPropertyObject("d9_SupplierZF").getStringValue();

            DataManagementUtil.getProperty(dataManagementService, irv, "d9_ManufacturerPN");
            String mfgPN= irv.getPropertyObject("d9_ManufacturerPN").getStringValue();
            if(mfgPN==null){
                mfgPN="";
            }
            DataManagementUtil.getProperty(dataManagementService, irv, "item_id");
            String itemId= irv.getPropertyObject("item_id").getStringValue();

            if(mfgName==null ||"".equalsIgnoreCase(mfgName)){
                log.info(itemId+ " 沒有中英文名");
                return itemId+ " 沒有中英文名";
            }


            List<PNSupplierInfo> pms= new ArrayList<>();
            PNSupplierInfo pn= new PNSupplierInfo();
            pn.setPartPn(itemId);
            pms.add(pn);
            List<PNSupplierInfo>  supplierInfos=sapSupplierMapper.selectInPartPn(pms,plant);
            if(supplierInfos!=null&&supplierInfos.size()>0){
                 for(PNSupplierInfo s:supplierInfos){
                    String sapItemId= s.getPartPn();
                    String spaMfgZh=s.getMfgZh();
                    String sapPlant=s.getPlant();
                    String sapMfgPn=s.getMfgPn();
                    if(sapMfgPn==null){
                        sapMfgPn="";
                    }
                    if(sapItemId.equalsIgnoreCase(itemId)&&sapPlant.equalsIgnoreCase(plant)&&sapMfgPn.equalsIgnoreCase(mfgPN)&&spaMfgZh.equalsIgnoreCase(mfgName)){
                        log.info(itemId + " 中英文名已存在,无需修改");
                        return itemId+  " 中英文名已存在,无需修改";
                    }
                 }
            }


            JCoFunction function = destination.getRepository().getFunction("ZRFC_DPBU_MM_0002");
            if (function == null) {
                log.info("The plant is not supported maker transfered!");
                return "The plant is not supported maker transfered!";
            }

            JCoParameterList tableList = function.getTableParameterList();
            JCoTable inputTable = tableList.getTable("INPUT_TAB");
            inputTable.appendRow();
            inputTable.setValue("WERKS", plant);
            inputTable.setValue("MATNR", itemId);
            inputTable.setValue("MAKERCODE", "");
            inputTable.setValue("MAKERPN", mfgPN);
            inputTable.setValue("MAKERNAME", mfgName);
            inputTable.setValue("ADDRESS", "");
            inputTable.setValue("CONTACTMAN", "");
            inputTable.setValue("FAXNUMBER","");
            inputTable.setValue("TELEPHONE", "");
            log.info("The params of the RFC ZRFC_DPBU_MM_0002 is:");
            log.info("WERKS:" + plant);
            log.info("MATNR:" + itemId);
            log.info("MAKERCODE:" + "");
            log.info("MAKERPN:" + mfgPN);
            log.info("MAKERNAME:" + mfgName);
            log.info("ADDRESS:" + "");
            log.info("CONTACTMAN:" + "");
            log.info("FAXNUMBER:" + "");
            log.info("TELEPHONE:" + "");
            function.execute(destination);
            JCoTable outputTable = tableList.getTable("INPUT_TAB");
            String result = String.valueOf(outputTable.getValue("REF1"));
            log.info("The result is :" + result);
            return result;
        } catch (Exception e) {
            log.error(e,e.getMessage());
            log.info("Error occurs when call RFC!");
            return "Error occurs when call RFC!" + e.getMessage();
        }

    }

}
