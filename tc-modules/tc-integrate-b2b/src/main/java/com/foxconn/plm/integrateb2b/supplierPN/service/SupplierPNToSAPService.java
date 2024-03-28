package com.foxconn.plm.integrateb2b.supplierPN.service;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.param.MakerPNRp;
import com.foxconn.plm.entity.param.PartPNRp;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.integrateb2b.dataExchange.constants.PlantConstants;
import com.foxconn.plm.integrateb2b.dataExchange.domain.BOMActionInfo;
import com.foxconn.plm.integrateb2b.dataExchange.domain.MaterialInfo;
import com.foxconn.plm.integrateb2b.dataExchange.mapper.DataExchangeMapper;
import com.foxconn.plm.integrateb2b.supplierPN.dao.SupplierPNToSAPMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class SupplierPNToSAPService {
    private static Log log = LogFactory.get();
    @Resource
    SupplierPNToSAPMapper mapper;

    @Autowired(required = false)
    public DataExchangeMapper dataExchangeMapper;

    @Resource
    private TcIntegrateClient tcIntegrateClient;

    public String syncSupplier(String changeNum, String plant) {
        String msg="";
        try {
            log.info("begin sync maker info " + changeNum + " " + plant);
            List<MakerPNRp> list = mapper.getMaterialList(changeNum, plant);
            for (MakerPNRp m : list) {
                m.setPlant(plant);
            }
            if (PlantConstants.isContain(PlantConstants.MNT_L10, plant)) {
                TCSOAServiceFactory tCSOAServiceFactory = null;
                List<MakerPNRp> removes = new ArrayList<>();
                try {
                    tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
                    DataManagementService dataManagementService = tCSOAServiceFactory.getDataManagementService();
                    List<BOMActionInfo> boms = dataExchangeMapper.getBomMaterialInfo(changeNum);
                    for (MakerPNRp m : list) {
                        for (BOMActionInfo b : boms) {
                            if (m.getMaterialNum().equalsIgnoreCase(b.getXfe_component_num())) {
                                String uid = b.getXfe_material_uid();
                                ServiceData sdDataset = dataManagementService.loadObjects(new String[]{uid});
                                ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
                                dataManagementService.refreshObjects(new ModelObject[]{irv});
                                TCUtils.getProperties(dataManagementService, irv, new String[]{"d9_MaterialGroup"});
                                String mg = replaceNull(irv.getPropertyObject("d9_MaterialGroup").getStringValue());
                                if ("B8X80".equalsIgnoreCase(mg)) {
                                    removes.add(m);
                                }
                            }
                        }
                    }
                    if (removes.size() > 0) {
                        list.removeAll(removes);
                    }
                } catch (Exception e) {
                    log.error(changeNum + " " + plant + " " + e.getMessage(), e);
                } finally {
                    if (tCSOAServiceFactory != null) {
                        tCSOAServiceFactory.logout();
                    }
                }
            }
            List<List<MakerPNRp>> parts=new ArrayList();
            int k=0;
            List<MakerPNRp> ls=new ArrayList<>();
            parts.add(ls);
            for(MakerPNRp m:list ){
                 ls.add(m);
                 k++;
                 if(k>120){
                     k=0;
                     ls=new ArrayList<>();
                     parts.add(ls);
                 }
            }

            for(List<MakerPNRp> ps:parts) {
                msg = tcIntegrateClient.postMakerPN(ps);
            }
            System.out.println(msg);
            log.info(msg);
            log.info("end sync maker info " + changeNum + " " + plant);
        }catch (Exception e){
            log.info(e.getMessage());
            log.error(e.getMessage(), e);
        }
        return msg;
    }


    private String replaceNull(String str) {
        if (str == null) {
            return "";
        } else {
            return str.trim();
        }

    }

}
