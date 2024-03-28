package com.foxconn.plm.integrateb2b.dataExchange.core;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.integrateb2b.dataExchange.domain.*;
import com.foxconn.plm.integrateb2b.dataExchange.mapper.DataExchangeMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

public abstract  class DataExchangeListener {
    private static Log log = LogFactory.get();
    @Value("${spring.b2b.url}")
    private String b2bUrl;

    @Autowired(required = false)
    public DataExchangeMapper dataExchangeMapper;


    public  abstract void dealwithBomAction(TCSOAServiceFactory tCSOAServiceFactory,List<BOMActionInfo> list,TransferOrder transferOrder)throws Exception;

    public  abstract void dealwithMaterails(TCSOAServiceFactory tCSOAServiceFactory,List<MaterialInfo> list,TransferOrder transferOrder)throws Exception;

    public String replaceNull(String str) {
        if (str == null) {
            return "";
        } else {
            return str.trim();
        }

    }

    public String buildTransferJson(TCSOAServiceFactory tCSOAServiceFactory, TransferOrder transferOrder) throws Exception {
        String transId = "" + new Date().getTime();
        HashMap map = new HashMap();
        map.put("changeNum", transferOrder.getChangNum());

        dataExchangeMapper.getBOMAction(map);
        List<BOMActionInfo> list = (List<BOMActionInfo>) map.get("cursor");
        Integer code = (Integer) map.get("code");
        String msg = (String) map.get("msg");
        if (code == null || code.intValue() != 1) {
            throw new Exception("比对bom差异失败" + msg);
        }
        dealwithBomAction(tCSOAServiceFactory,list,transferOrder);

        List<MaterialInfo> materialInfos = dataExchangeMapper.getMaterialInfo(transferOrder.getChangNum());
        dealwithMaterails(tCSOAServiceFactory,materialInfos,transferOrder);

        JSONObject obj = new JSONObject();
        if (materialInfos != null && materialInfos.size() > 0) {
            JSONArray parts = new JSONArray();
            obj.put("part", parts);
            for (MaterialInfo m : materialInfos) {
                JSONObject o = new JSONObject();
                o.put("materialNo", m.getMaterialNum());
                o.put("materialtype", m.getMaterialType());
                o.put("descriptionEN", replaceNull(m.getMaterialDescriptionEn()));
                String zf = replaceNull(m.getMaterialDescriptionZf());
                if (zf == null || "".equalsIgnoreCase(zf.trim())) {
                    zf = replaceNull(m.getMaterialDescriptionEn());
                }
                zf= ZhConverterUtil.toTraditional(zf);
                o.put("descriptionZF", zf);
                o.put("baseunit", replaceNull(m.getMaterialBaseUnit()).toUpperCase(Locale.ENGLISH));
                o.put("rev", getMaterialRev(m));
                o.put("materialGroup", replaceNull(m.getMaterialGroup()));
                o.put("mfrPN", replaceNull(m.getMaterialMfgPn()));
                o.put("mfrID", replaceNull(m.getMaterialMfgId()));
                o.put("procurementType", replaceNull(m.getMaterialProcurementType()));
                o.put("grossWeight", replaceNull(m.getMaterialGrossWeight()));
                o.put("netWeight", replaceNull(m.getMaterialNetWeight()));
                o.put("weightUnit", replaceNull(m.getMaterialWeightUnit()).toUpperCase(Locale.ENGLISH));
                parts.add(o);
            }
        }
        if (list != null && list.size() > 0) {
            JSONArray boms = new JSONArray();
            obj.put("bom", boms);
            HashMap<String, JSONObject> mp = new HashMap<>();
            for (BOMActionInfo b : list) {
                String parentNum = b.getXfe_mm_num();
                JSONObject parentObj = mp.get(parentNum);
                if (parentObj == null) {
                    parentObj = new JSONObject();
                    parentObj.put("parentlNo", b.getXfe_mm_num());
                    parentObj.put("bomUsage", b.getXfe_bom_usage());
                    parentObj.put("alternativeBOM", b.getXfe_alternative_bom());
                    parentObj.put("basicUnit", b.getXfe_base_quantity());
                    JSONArray subs = new JSONArray();
                    parentObj.put("children", subs);
                    mp.put(parentNum, parentObj);
                    boms.add(parentObj);
                }
                JSONObject o = new JSONObject();
                o.put("childNo", b.getXfe_component_num());
                o.put("findnum", b.getXfe_find_num());
                o.put("itemText", replaceNull(b.getXfe_item_text()));
                o.put("itemCategory", replaceNull(b.getXfe_item_category()));
                o.put("componentQty", b.getXfe_component_qty());
                o.put("componentUnitOfMeasure", b.getXfe_unit().toUpperCase(Locale.ENGLISH));
                o.put("sortstring", replaceNull(b.getXfe_alt_code()));
                o.put("altItemGroup", replaceNull(b.getXfe_alt_group()));
                o.put("priority", replaceNull(b.getXfe_priority()));
                o.put("strategy", replaceNull(b.getXfe_strategy()));
                o.put("usageProb", replaceNull(b.getXfe_usage_prob()));
                o.put("location", replaceNull(formatLocaltion(b.getXfe_component_num(),b.getXfe_component_qty(), b.getXfe_location(),b.getXfe_find_num(),parentNum,transferOrder.getChangNum())));
                o.put("sysACD", b.getXfe_action());
                parentObj.getJSONArray("children").add(o);
            }
        }

        obj.put("changeNo", transferOrder.getChangNum());
        obj.put("plant", transferOrder.getPlantCode());
        obj.put("status", transferOrder.getChangeStatus());
        obj.put("validFrom", DateUtil.format(DateUtil.parse(transferOrder.getEffectDate()), "yyyyMMdd"));
        obj.put("descriptionOfChange", transferOrder.getChangeDescr());
        obj.put("originator", transferOrder.getOwner());
        obj.put("transId", transId);

        return obj.toJSONString();
    }


    public String getMaterialRev(MaterialInfo materialInfo) throws Exception {
        String rev = materialInfo.getMaterialRev();
        if (rev == null) {
            rev = "";
        }
        return rev;
    }



    public String formatLocaltion(String itemNumber,String quantity, String location,String findNum,String parentNum,String ecnNO) throws Exception {
        if (location == null || "".equalsIgnoreCase(location.trim())) {
            return "";
        }
        location = location.trim();
        if (location.startsWith(",")) {
            location = location.substring(1);
        }
        if (location.endsWith(",")) {
            location = location.substring(0, location.length() - 1);
        }

        String[] m = location.split(",");
        if (m == null || m.length <= 0) {
            return "";
        }

        int length=m.length;
        Float f=Float.parseFloat(quantity);
        Float f0=f/length;
        String f0Str=f0.toString();
        String f0Str2=null;
        String f0StrTmp="";

        if(f0Str.indexOf("E")>-1||f0Str.indexOf("e")>-1) {
            throw new Exception (itemNumber+"计算location失败");
        }

        if(f0Str.indexOf(".")>-1) {
            f0StrTmp=f0Str.substring(f0Str.lastIndexOf(".")+1);
        }

        if(f0StrTmp.length()>3&&length>1) {
            String f0Strtt=""+Integer.parseInt(f0Str.substring(0, f0Str.lastIndexOf(".")));
            if(f0Strtt.equalsIgnoreCase("0")) {
                f0Str=f0Str.substring(0, f0Str.lastIndexOf(".")+3);
                String tmp=""+Float.parseFloat(f0Str)*(length-1);
                if(tmp.indexOf(".")>0&&(tmp.substring(tmp.lastIndexOf(".")+1).length()>3)) {
                    tmp=tmp.substring(0, tmp.lastIndexOf(".")+3);
                }
                BigDecimal b=NumberUtil.sub(""+f,""+Float.parseFloat(tmp));
                f0Str2=""+b.floatValue();
            }else {
                f0Str=f0Strtt;
                f0Str2=(f-(Integer.parseInt(f0Str)*(length-1)))+"";
            }
        }
        if(Float.parseFloat(f0Str)<=0) {
            throw new Exception (itemNumber+"计算location失败");
        }

        if(f0Str.indexOf(".")>0) {
            if(f0Str.substring(f0Str.lastIndexOf(".")+1).length()>3) {
                throw new Exception (itemNumber+"计算location失败");
            }
        }

        if(f0Str2!=null&&f0Str2.indexOf(".")>0) {
            if(f0Str2.substring(f0Str2.lastIndexOf(".")+1).length()>3) {
                throw new Exception (itemNumber+"计算location失败");
            }
        }

        String rs = "";
        String msg="";
        int cnt=0;
        if(f0Str2==null){
            for (String str : m) {
                if(cnt>=99){
                    msg="location个数超过了100";
                }else{
                    rs += str + "-" + f0Str + ",";
                }
                cnt++;
            }
        }else{
            for(int i=0;i<m.length-1;i++){
                String str = m[i];
                if(cnt>=99){
                    msg="location个数超过了100";
                }else {
                    rs += str + "-" + f0Str + ",";
                }
                cnt++;
            }
            if(cnt>=99){
                msg="location个数超过了100";
            }else {
                rs += m[m.length - 1] + "-" + f0Str2 + ",";
            }
        }
        if (rs.endsWith(",")) {
            rs = rs.substring(0, rs.length() - 1);
        }

        Map<String,String> mp=new HashMap<>();
        for(String s:m){
            if(mp.get(s)!=null){
                msg="location有重复";
                break;
            }else{
                mp.put(s,s);
            }
        }

        if(!("".equalsIgnoreCase(msg))){
            sendMail(ecnNO+" "+parentNum+" "+findNum+" "+itemNumber+ " "+msg );
        }

        return rs;
    }


    public void updateTransferOrder(TransferOrderResp transferOrderResp) throws Exception {
        dataExchangeMapper.updateTransferOrder(transferOrderResp);
    }

    public PostB2BResp postJsonData(String jsonStr) throws Exception {
        log.info("begin post json data to b2b:" + jsonStr);
        Map params = new HashMap();
        params.put("dataContent", jsonStr);
        String msg="";
        msg = HttpUtil.post(b2bUrl, params);
        PostB2BResp postRp = JSONObject.parseObject(msg, PostB2BResp.class);
        log.info("post result:" + msg);
        return postRp;
    }


    public abstract void sendMail(String  msg) throws Exception;

}
