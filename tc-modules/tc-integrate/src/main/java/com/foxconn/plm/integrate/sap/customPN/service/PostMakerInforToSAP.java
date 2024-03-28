package com.foxconn.plm.integrate.sap.customPN.service;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.integrate.sap.customPN.mapper.CustomPNMapper;
import com.foxconn.plm.integrate.sap.maker.domain.MakerInfor;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoTable;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ItemRevision;

import java.util.List;

public class PostMakerInforToSAP {
    private static Log log = LogFactory.get();
    private ItemRevision rev = null;
    private MakerInfor makerInfor = null;
    private DataManagementService dataManagementService;
    private String plant;
    private JCoDestination destination;
    private CustomPNMapper customPNMapper;


    public PostMakerInforToSAP(DataManagementService dataManagementService, ItemRevision rev, String plant, JCoDestination destination, CustomPNMapper customPNMapper) {
        this.rev = rev;
        this.dataManagementService = dataManagementService;
        this.destination = destination;
        this.customPNMapper = customPNMapper;
        this.plant = plant;
    }

    private void setMaker() throws Exception {
        dataManagementService.refreshObjects(new ModelObject[]{rev});
        TCUtils.getProperty(dataManagementService, rev, "d9_MFGAddress");
        String address = StringUtil.replaceNull(rev.getPropertyObject("d9_MFGAddress").getStringValue());
        TCUtils.getProperty(dataManagementService, rev, "d9_MFGContacts");
        String contactMan = StringUtil.replaceNull(rev.getPropertyObject("d9_MFGContacts").getStringValue());

        TCUtils.getProperty(dataManagementService, rev, "d9_SupplierZF");
        String makerName = StringUtil.replaceNull(rev.getPropertyObject("d9_SupplierZF").getStringValue());

        String makerCode = "";
        List<String> mks = customPNMapper.selectMakerCode(makerName);
        if (mks != null && mks.size() > 0) {
            makerCode = mks.get(0);
        }

        TCUtils.getProperty(dataManagementService, rev, "d9_MFGFax");
        String faxNumber = StringUtil.replaceNull(rev.getPropertyObject("d9_MFGFax").getStringValue());

        TCUtils.getProperty(dataManagementService, rev, "d9_ManufacturerPN");
        String makerPN = StringUtil.replaceNull(rev.getPropertyObject("d9_ManufacturerPN").getStringValue());

        TCUtils.getProperty(dataManagementService, rev, "d9_MFGTel");
        String telephone = StringUtil.replaceNull(rev.getPropertyObject("d9_MFGTel").getStringValue());

        TCUtils.getProperty(dataManagementService, rev, "item_id");
        String partNumber = StringUtil.replaceNull(rev.getPropertyObject("item_id").getStringValue());
        makerInfor = new MakerInfor();
        makerInfor.setPuid(rev.getUid());
        makerInfor.setAddress(address);
        makerInfor.setContactMan(contactMan);
        makerInfor.setFaxNumber(faxNumber);
        makerInfor.setMakerCode(makerCode);
        makerInfor.setMakerName(makerName);
        makerInfor.setPartNumber(partNumber);
        makerInfor.setMakerPN(makerPN);
        makerInfor.setTelephone(telephone);

    }


    public MakerInfor post() {
        try {
            setMaker();
            if (makerInfor == null) {
                makerInfor = new MakerInfor();
                log.info("get imteminfo failed !");
                makerInfor.setSyncRs("get imteminfo failed !");
                makerInfor.setPuid(rev.getUid());
                return makerInfor;
            }

            if (makerInfor.getMakerName() == null || "".equalsIgnoreCase(makerInfor.getMakerName())
                    || makerInfor.getMakerCode() == null || "".equalsIgnoreCase(makerInfor.getMakerCode())
                    || makerInfor.getMakerPN() == null || "".equalsIgnoreCase(makerInfor.getMakerPN())
            ) {
                makerInfor.setSyncRs("The part's maker infor is empty,so it won't be post to sap!");
                return makerInfor;
            }


            String rs = post0(makerInfor, plant);
            makerInfor.setSyncRs(rs);
            return makerInfor;

        } catch (Exception e) {
            if (makerInfor == null) {
                makerInfor = new MakerInfor();
                makerInfor.setPuid(rev.getUid());
            }
            log.info(e.getMessage());
            makerInfor.setSyncRs(e.getMessage());
            return makerInfor;
        }
    }

    private String post0(MakerInfor makerInfor, String plant) {
        try {
            JCoFunction function = destination.getRepository().getFunction("ZRFC_DPBU_MM_0002");
            if (function == null) {
                log.info("The plant is not supported maker transfered!");
                return "The plant is not supported maker transfered!";
            }
            JCoParameterList tableList = function.getTableParameterList();
            JCoTable inputTable = tableList.getTable("INPUT_TAB");
            inputTable.appendRow();
            inputTable.setValue("WERKS", plant);
            inputTable.setValue("MATNR", makerInfor.getPartNumber());
            inputTable.setValue("MAKERCODE", makerInfor.getMakerCode());
            inputTable.setValue("MAKERPN", makerInfor.getMakerPN());
            inputTable.setValue("MAKERNAME", makerInfor.getMakerName());
            inputTable.setValue("ADDRESS", makerInfor.getAddress());
            inputTable.setValue("CONTACTMAN", makerInfor.getContactMan());
            inputTable.setValue("FAXNUMBER", makerInfor.getFaxNumber());
            inputTable.setValue("TELEPHONE", makerInfor.getTelephone());
            log.info("The params of the RFC ZRFC_DPBU_MM_0002 is:");
            log.info("WERKS:" + plant);
            log.info("MATNR:" + makerInfor.getPartNumber());
            log.info("MAKERCODE:" + makerInfor.getMakerCode());
            log.info("MAKERPN:" + makerInfor.getMakerPN());
            log.info("MAKERNAME:" + makerInfor.getMakerName());
            log.info("ADDRESS:" + makerInfor.getAddress());
            log.info("CONTACTMAN:" + makerInfor.getContactMan());
            log.info("FAXNUMBER:" + makerInfor.getFaxNumber());
            log.info("TELEPHONE:" + makerInfor.getTelephone());
            function.execute(destination);
            JCoTable outputTable = tableList.getTable("INPUT_TAB");
            String result = String.valueOf(outputTable.getValue("REF1"));
            log.info("The result is :" + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Error occurs when call RFC!");
            return "Error occurs when call RFC!" + e.getMessage();
        }

    }


}

