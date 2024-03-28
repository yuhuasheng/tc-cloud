package com.foxconn.plm.integrate.sap.customPN.utils;

import com.foxconn.plm.integrate.sap.customPN.domain.rp.CustomPartRp;
import com.foxconn.plm.integrate.sap.customPN.mapper.CustomPNMapper;
import com.foxconn.plm.integrate.sap.customPN.view.SapView;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.*;

public class ViewUtils {


    public static Map<String, String> viewCode = new HashMap<>();

    static {
        viewCode.put("sd", "sd");
        viewCode.put("mrp", "mrp");
        viewCode.put("storage", "storage");
        viewCode.put("accounting", "accounting");
        viewCode.put("purchasing", "purchasing");
        viewCode.put("classification", "classification");
    }


    /**
     * 判断 是不是要抛view
     *
     * @param plant
     * @param mtlType
     * @param vCode
     * @return
     * @throws Exception
     */
    public boolean getNeedPost(CustomPNMapper customPNMapper, String plant, String mtlType, String vCode) throws Exception {

        String result = "";
        List<String> rs = customPNMapper.selectNeedPost(mtlType, viewCode.get(vCode), plant);
        if (rs != null && rs.size() > 0) {
            result = rs.get(0);
        }
        return "1".equals(result);
    }


    /**
     * 判断需要抛转那些View 到SAP
     *
     * @param destination
     * @param applicationPartPojo
     * @return
     * @throws Exception
     */
    public int needSendView(JCoDestination destination, CustomPartRp applicationPartPojo) throws Exception {
        JCoFunction function = destination.getRepository().getFunctionTemplate("ZRFC_DPBU_CREATEVIEWS").getFunction();
        JCoParameterList importParameterList = function.getImportParameterList();
        importParameterList.setValue("MATERIAL", applicationPartPojo.getMaterialNumber());
        String newPlant = applicationPartPojo.getPlant();
        if (newPlant == null) {
            newPlant = "CHMB";
        }
        try {
            newPlant = newPlant.trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (newPlant.indexOf(",") > -1) {
            newPlant = newPlant.split(",")[0].trim();
        }
        importParameterList.setValue("PLANT", newPlant);
        function.execute(destination);
        JCoParameterList output = function.getExportParameterList();
        System.out.println("plant:" + applicationPartPojo.getPlant() + ",partNumber:" + applicationPartPojo.getMaterialNumber() + ",result:" + output.getValue("FLAG").toString());
        return Integer.parseInt(output.getValue("FLAG").toString());
    }


    public SapView getView(CustomPNMapper customPNMapper, String viewClassName, CustomPartRp applicationPartPojo) {
        String attrName = "";
        String attrValue = "";
        try {
            Class clazz = Class.forName(viewClassName);
            Object sapView = clazz.newInstance();
            Field[] fields = clazz.getDeclaredFields();
            Method method = null;
            for (int i = 0; i < fields.length; i++) {
                attrName = fields[i].getName();
                attrValue = getValue(customPNMapper, attrName, applicationPartPojo);
                attrName = "set" +
                        attrName.replaceFirst(".{1}", attrName
                                .substring(0, 1).toUpperCase(Locale.ENGLISH));
                method = clazz.getDeclaredMethod(attrName, new Class[]{String.class});
                method.invoke(sapView, new Object[]{attrValue});
            }
            return (SapView) sapView;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    String getValue(CustomPNMapper customPNMapper, String attrName, CustomPartRp applicationPartPojo) {
        return replaceNull(getValueProcess(customPNMapper, attrName, applicationPartPojo));
    }


    private String getValueProcess(CustomPNMapper customPNMapper, String attrName, CustomPartRp applicationPartPojo) {
        String ruleType = CustomPNPropertites.props.getProperty("post_sap_lh_[getvaluerule]." +
                attrName);
        if (ruleType == null) {
            return null;
        }
        if (ruleType.startsWith("default")) {
            return ruleType.replaceAll("default_", "");
        }
        return getValueFromDB(customPNMapper, ruleType, attrName, applicationPartPojo);
    }


    private String getValueFromDB(CustomPNMapper customPNMapper, String ruleType, String attrName, CustomPartRp applicationPartPojo) {
        String result = "";
        List<String> rs = null;
        if (ruleType.equals("by_plant_mtlType")) {
            result = getSqlByPlantMtlType(customPNMapper, applicationPartPojo.getPlant(), attrName, applicationPartPojo.getMaterialType());
        } else if (ruleType.equals("by_mtlGroup")) {
            result = getSqlByMtlGroup(customPNMapper, applicationPartPojo.getMaterialGroup(), attrName);
        } else if (ruleType.equals("by_mtlType")) {
            result = getSqlByMtlType(customPNMapper, attrName, applicationPartPojo.getMaterialType());
        } else if (ruleType.equals("by_plant")) {
            result = getSqlByPlant(customPNMapper, applicationPartPojo.getPlant(), attrName);
        } else if (ruleType.equals("by_partSource")) {
            result = getSqlByPartSource(customPNMapper, attrName, applicationPartPojo.getPartSource());
        }

        return result;
    }


    private String getSqlByPartSource(CustomPNMapper customPNMapper, String name, String partSource) {
        String result = "";
        List<Map<String, Object>> rs = customPNMapper.getSqlByPartSource(partSource);
        if (rs != null && rs.size() > 0) {
            Map<String, Object> map = rs.get(0);
            Set<String> keys = map.keySet();
            for (String key : keys) {
                if (key.equalsIgnoreCase(name)) {
                    result = (String) map.get(key);
                    break;
                }
            }
        }
        if (result == null) {
            result = "";
        }
        return result;

    }


    private String getSqlByMtlType(CustomPNMapper customPNMapper, String name, String mtlType) {

        String result = "";
        List<Map<String, Object>> rs = customPNMapper.getSqlByMtlType(mtlType);
        if (rs != null && rs.size() > 0) {
            Map<String, Object> map = rs.get(0);
            Set<String> keys = map.keySet();
            for (String key : keys) {
                if (key.equalsIgnoreCase(name)) {
                    result = (String) map.get(key);
                    break;
                }
            }
        }
        if (result == null) {
            result = "";
        }
        return result;

    }

    private String getSqlByPlant(CustomPNMapper customPNMapper, String plantCode, String name) {
        String result = "";
        List<Map<String, Object>> rs = customPNMapper.getSqlByPlant(plantCode);
        if (rs != null && rs.size() > 0) {
            Map<String, Object> map = rs.get(0);
            Set<String> keys = map.keySet();
            for (String key : keys) {
                if (key.equalsIgnoreCase(name)) {
                    result = (String) map.get(key);
                    break;
                }
            }
        }
        if (result == null) {
            result = "";
        }
        return result;

    }


    private String getSqlByMtlGroup(CustomPNMapper customPNMapper, String mtlGrp, String name) {
        String result = "";
        List<Map<String, Object>> rs = customPNMapper.getSqlByMtlGroup(mtlGrp);
        if (rs != null && rs.size() > 0) {
            Map<String, Object> map = rs.get(0);
            Set<String> keys = map.keySet();
            for (String key : keys) {
                if (key.equalsIgnoreCase(name)) {
                    result = (String) map.get(key);
                    break;
                }
            }
        }
        if (result == null) {
            result = "";
        }
        return result;
    }


    private String getSqlByPlantMtlType(CustomPNMapper customPNMapper, String plantCode, String name, String mtlType) {
        String result = "";
        List<Map<String, Object>> rs = customPNMapper.getSqlByPlantMtlType(mtlType, plantCode);
        if (rs != null && rs.size() > 0) {
            Map<String, Object> map = rs.get(0);
            Set<String> keys = map.keySet();
            for (String key : keys) {
                if (key.equalsIgnoreCase(name)) {
                    result = (String) map.get(key);
                    break;
                }
            }
        }
        if (result == null) {
            result = "";
        }
        return result;
    }


    public static String replaceNull(String str) {
        if (str == null) {
            str = "";
        } else {
            str = str.trim();
        }
        return str;
    }

}
