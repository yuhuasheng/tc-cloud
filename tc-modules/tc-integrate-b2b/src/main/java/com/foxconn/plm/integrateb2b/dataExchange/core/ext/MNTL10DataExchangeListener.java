package com.foxconn.plm.integrateb2b.dataExchange.core.ext;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.param.PartPNRp;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.integrateb2b.dataExchange.core.DataExchangeListener;
import com.foxconn.plm.integrateb2b.dataExchange.domain.BOMActionInfo;
import com.foxconn.plm.integrateb2b.dataExchange.domain.MaterialInfo;
import com.foxconn.plm.integrateb2b.dataExchange.domain.TransferOrder;
import com.foxconn.plm.integrateb2b.dataExchange.mapper.DataExchangeMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service("mnt10DataExchangeListener")
@Scope("prototype")
public class MNTL10DataExchangeListener extends DataExchangeListener {
    private static Log log = LogFactory.get();
    @Resource
    private TcIntegrateClient tcIntegrateClient;

    @Resource
    TcMailClient tcMail;

    @Autowired(required = false)
    public DataExchangeMapper dataExchangeMapper;
    @Override
    public void dealwithBomAction(TCSOAServiceFactory tCSOAServiceFactory, List<BOMActionInfo> list, TransferOrder transferOrder) throws Exception {
        List<BOMActionInfo> removes= new ArrayList<>();
        DataManagementService dataManagementService= tCSOAServiceFactory.getDataManagementService();
        for (BOMActionInfo b : list) {
            String uid = b.getXfe_material_uid();
            ServiceData sdDataset = dataManagementService.loadObjects(new String[]{uid});
            ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
            dataManagementService.refreshObjects(new ModelObject[]{irv});
            TCUtils.getProperties(dataManagementService, irv, new String[]{"d9_MaterialGroup"});
            String mg=replaceNull(irv.getPropertyObject("d9_MaterialGroup").getStringValue());
            if("B8X80".equalsIgnoreCase(mg)){
                log.info("need remove bom ========> "+b.getXfe_mm_num()+"  "+b.getXfe_component_num()+" "+b.getXfe_action());
                removes.add(b);
            }
        }
        //過濾掉PCBA下階
        if(removes.size()>0){
            list.removeAll(removes);
        }
    }

    @Override
    public void dealwithMaterails(TCSOAServiceFactory tCSOAServiceFactory,List<MaterialInfo> list,TransferOrder transferOrder) throws Exception {
        if(list==null){
            list= new ArrayList<>();
        }
        HashMap<String,MaterialInfo>  mps=new HashMap<>();
        for(MaterialInfo m:list){
            mps.put(m.getMaterialNum(),m);
        }
        DataManagementService dataManagementService= tCSOAServiceFactory.getDataManagementService();
        List<MaterialInfo> ls2=getBomParts(transferOrder.getChangNum(),dataManagementService);
        for(MaterialInfo m:ls2){
            mps.put(m.getMaterialNum(),m);
        }
        list.clear();
        Set<String> keys= mps.keySet();
        for(String key:keys){
            list.add(mps.get(key));
        }

        //過濾掉PCBA下階
        List<MaterialInfo> removes= new ArrayList<>();
        List<BOMActionInfo> boms= dataExchangeMapper.getBomMaterialInfo(transferOrder.getChangNum());
        for(MaterialInfo m:list){
            for(BOMActionInfo b:boms) {
                if (m.getMaterialNum().equalsIgnoreCase(b.getXfe_component_num())) {
                    String uid = b.getXfe_material_uid();
                    ServiceData sdDataset = dataManagementService.loadObjects(new String[]{uid});
                    ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
                    dataManagementService.refreshObjects(new ModelObject[]{irv});
                    TCUtils.getProperties(dataManagementService, irv, new String[]{"d9_MaterialGroup"});
                    String mg=replaceNull(irv.getPropertyObject("d9_MaterialGroup").getStringValue());
                    if("B8X80".equalsIgnoreCase(mg)){
                        removes.add(m);
                    }
                }
            }
        }
        if(removes.size()>0){
            list.removeAll(removes) ;
        }

        //過濾掉SAP已經存在的物料
        removes= new ArrayList<>();
        List<List<PartPNRp>> parts=new ArrayList();
        List<PartPNRp>  notInsaps=new ArrayList<>();
        int k=0;
        List<PartPNRp> ls=new ArrayList<>();
        parts.add(ls);
        for(MaterialInfo p:list){
            PartPNRp pr=new PartPNRp();
            pr.setItemNumber(p.getMaterialNum());
            pr.setPlant(transferOrder.getPlantCode());
            ls.add(pr);
            k++;
            if(k>100){
                k=0;
                ls=new ArrayList<>();
                parts.add(ls);
            }
        }
        for(List<PartPNRp> ps:parts) {
            List<PartPNRp> tmp = tcIntegrateClient.isExistInSAP(ps);
            if(tmp!=null&&tmp.size()>0){
                notInsaps.addAll(tmp);
            }
        }
        for(MaterialInfo m:list){
            int f=0;
            for(PartPNRp p:notInsaps){
                if(m.getMaterialNum().equalsIgnoreCase(p.getItemNumber())){
                    f=1;
                    break;
                }
            }
            if(f==0){
                removes.add(m);
            }
        }
        if(removes.size()>0){
            list.removeAll(removes) ;
        }

    }

    private List<MaterialInfo> getBomParts( String changeNum,DataManagementService dataManagementService) throws Exception {
        List<MaterialInfo> parts = new ArrayList<>();
        List<BOMActionInfo> boms= dataExchangeMapper.getBomMaterialInfo(changeNum);
        Map<String,String>  mm=new HashMap<>();
        for (BOMActionInfo b : boms) {
            String materialNum = b.getXfe_mm_num();
            String uid = b.getXfe_material_uid();
            ServiceData sdDataset = dataManagementService.loadObjects(new String[]{uid});
            ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
            MaterialInfo part = getPartInfor(materialNum,irv,dataManagementService);
            if(mm.get(part.getMaterialNum())==null) {
                parts.add(part);
                mm.put(part.getMaterialNum(),part.getMaterialNum());
            }
            if("B8X80".equalsIgnoreCase(part.getMaterialGroup())){
                continue;
            }
            materialNum = b.getXfe_component_num();
            uid = b.getXfe_component_uid();
            sdDataset = dataManagementService.loadObjects(new String[]{uid});
            ItemRevision childIrv = (ItemRevision) sdDataset.getPlainObject(0);
            MaterialInfo childPart = getPartInfor(materialNum,childIrv,dataManagementService);
            if(mm.get(childPart.getMaterialNum())==null) {
                parts.add(childPart);
                mm.put(childPart.getMaterialNum(),childPart.getMaterialNum());
            }

        }
        return parts;
    }

    private MaterialInfo getPartInfor(String materialNum,ItemRevision irv,DataManagementService dataManagementService) throws Exception{
        dataManagementService.refreshObjects(new ModelObject[]{irv});
        TCUtils.getProperties(dataManagementService, irv, new String[]{"d9_DescriptionSAP", "d9_EnglishDescription", "d9_ChineseDescription", "item_revision_id", "d9_Un", "d9_MaterialGroup", "d9_MaterialType", "d9_ManufacturerPN", "d9_ManufacturerID", "d9_ProcurementMethods", "d9_SAPRev"});
        String mg=replaceNull(irv.getPropertyObject("d9_MaterialGroup").getStringValue());
        MaterialInfo part = new MaterialInfo();
        part.setMaterialNum(materialNum);
        String sapDescr = irv.getPropertyObject("d9_DescriptionSAP").getStringValue();
        String enDescr = irv.getPropertyObject("d9_EnglishDescription").getStringValue();
        if (sapDescr == null || "".equalsIgnoreCase(sapDescr)) {
            sapDescr = enDescr;
        }
        part.setMaterialDescriptionEn(replaceNull(sapDescr));
        String descrZF = replaceNull(irv.getPropertyObject("d9_ChineseDescription").getStringValue());
        if (descrZF == null || "".equalsIgnoreCase(descrZF)) {
            descrZF = part.getMaterialDescriptionEn();
        }
        part.setMaterialDescriptionZf(descrZF);
        part.setMaterialBaseUnit(replaceNull(irv.getPropertyObject("d9_Un").getStringValue()));
        part.setMaterialType(replaceNull(irv.getPropertyObject("d9_MaterialType").getStringValue()));
        part.setMaterialRev(replaceNull(irv.getPropertyObject("item_revision_id").getStringValue()));
        part.setSapRev(replaceNull(irv.getPropertyObject("d9_SAPRev").getStringValue()));

        part.setMaterialGroup(mg);
        part.setMaterialMfgPn(replaceNull(irv.getPropertyObject("d9_ManufacturerPN").getStringValue()));
        part.setMaterialMfgId(replaceNull(irv.getPropertyObject("d9_ManufacturerID").getStringValue()));
        part.setMaterialProcurementType(replaceNull(irv.getPropertyObject("d9_ProcurementMethods").getStringValue()));
        part.setMaterialGrossWeight("1");
        part.setMaterialNetWeight("1");
        part.setMaterialWeightUnit("G");
        return part;
    }

    @Override
    public String getMaterialRev(MaterialInfo materialInfo) throws Exception {
        String rev = materialInfo.getSapRev();
        if (rev == null) {
            rev = "";
        }
        return rev;
    }


    @Override
    public void sendMail(String msg) throws Exception {

        JSONObject httpmap = new JSONObject();
        httpmap.put("sendTo", "enisa.h.liu@foxconn.com,shu-mei.shen@foxconn.com");
        httpmap.put("sendCc", "leky.p.li@foxconn.com");
        httpmap.put("subject", "【TC 抛转SAP 】loation信息不正确，请及时处理！");
        String message ="location 信息:"+msg ;
        httpmap.put("htmlmsg", message);
        tcMail.sendMail3Method(httpmap.toJSONString());

    }

}
