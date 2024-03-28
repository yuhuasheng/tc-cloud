package com.foxconn.plm.integrate.sap.maker.service;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.integrate.sap.customPN.mapper.CustomPNMapper;
import com.foxconn.plm.integrate.sap.customPN.utils.ConnectPoolUtils;
import com.foxconn.plm.integrate.sap.customPN.utils.SAPConstants;
import com.foxconn.plm.integrate.sap.maker.domain.Maker;
import com.foxconn.plm.integrate.sap.maker.mapper.MakerMapper;
import com.foxconn.plm.utils.date.DateUtil;
import com.sap.conn.jco.*;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SynMakerschedule {
    private static Log log = LogFactory.get();
      @Autowired(required = false)
       private MakerMapper makerMapper;

    @Autowired(required = false)
    private CustomPNMapper customPNMapper;


    /**
     * 同步供應商中英文名信息到SAP
     * */
      @XxlJob("TcSynMakerSchedule")
       public void timedTask() {
          callRFC();
        }



       public  void callRFC() {
           JCoDestination destination=null;
           List<Maker> makerList = new ArrayList();
           try {
              if(SAPConstants.SAP_IP==null||"".equalsIgnoreCase(SAPConstants.SAP_IP)){
                throw new Exception("ahost is null");
              }
              destination = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED);
              JCoFunction function = destination.getRepository().getFunctionTemplate("ZRFC_MM_MARKER").getFunction();

              JCoParameterList imParameterList = function.getImportParameterList();
              JCoParameterList exParameterList = function.getExportParameterList();
              JCoTable table = function.getTableParameterList().getTable("TAB_OUT");
              Date nowDate = new Date();
              Date aDate = DateUtil.getNextDay(nowDate);

              imParameterList.setValue("DATE",aDate);
              function.execute(destination);

              if (exParameterList != null) {
                  log.info("SynMakerschedule export is not null");
                System.out.println("export is not null");
              }
               log.info("SynMakerschedule The table DM_VM_MARA Fields size:" + table.getNumRows() + " and record size:" + table.getFieldCount());
               System.out.println("The table DM_VM_MARA Fields size:" + table.getNumRows() + " and record size:" + table.getFieldCount());
               System.out.println("The table DM_VM_MARA Fields");
               log.info("SynMakerschedule The table DM_VM_MARA Fields");

               System.out.println("\n");
              for (int i = 0; i < table.getNumRows(); ++i) {
                table.setRow(i);
                Maker maker = new Maker();
                String idString = (String)table.getValue(0);
                maker.setId(idString);
                String addresssString = (String)table.getValue(1);
                maker.setAddress(addresssString);
                String makernameString = (String)table.getValue(2);
                maker.setName(makernameString);
                makerList.add(maker);
              }

              System.out.print(makerList.size());
              if ((makerList != null) && (makerList.size() != 0))
              {
                operationMaker(makerList);
              }
              else {
                log.info(aDate + "has no new maker");
              }
        }catch (Exception e) {
            log.info("SynMakerschedule Exception " + e.getMessage());
            e.printStackTrace();
               XxlJobHelper.handleFail(e.getLocalizedMessage());
        }
    }


    public String operationMaker(List<Maker> makerlist) {
        if ((makerlist != null) && (makerlist.size() != 0)) {
            log.debug("makerlist = " + makerlist.size());
            for (int i = 0; i < makerlist.size(); ++i)
            {
                addMakerInfo(makerlist.get(i));
            }
        }
        return null;
    }

    private  String addMakerInfo(Maker maker)
    {
        String addOperationflag = "";
        try
        {
            log.info("start add maker " + maker.getId() + " " + maker.getName());
            makerMapper.deleteMakerInl(maker);
            makerMapper.deleteMakerSas(maker);
            makerMapper.addMakerInl(maker);
            makerMapper.addMakerSas(maker);
            log.info(" add maker " + maker.getId() + " " + maker.getName() + "sucesses");
            addOperationflag = "sucesses";
        }
        catch (Exception e)
        {
            addOperationflag = "error";
        }
        return addOperationflag;
    }





}
