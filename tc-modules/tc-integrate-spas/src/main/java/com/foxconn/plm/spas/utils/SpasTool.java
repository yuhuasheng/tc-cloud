package com.foxconn.plm.spas.utils;

import com.foxconn.plm.entity.constants.TCPreferenceConstant;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;

public class SpasTool {



    public static  boolean isNoAcountDept(TCSOAServiceFactory tCSOAServiceFactory, String bu , String deptName) throws Exception {
        String[] noAccountDepts = TCUtils.getTCPreferences(tCSOAServiceFactory.getPreferenceManagementService(),
                TCPreferenceConstant.D9_TC_NOACCOUNT_DEPARTMENT);

        for(String ls:noAccountDepts){
            if(ls.startsWith(bu)){
                String[]  m= ls.split("=")[1].split(",");
                for(String s:m){
                    if(s.equalsIgnoreCase(deptName)){
                        return true;
                    }
                }

            }
        }
        return false;
    }

    public  static boolean  isNoManPowerDept(String deptForlderName){
        if(deptForlderName.startsWith("SPM")||deptForlderName.startsWith("PM")||deptForlderName.startsWith("TCFR")){
            return true;
        }
        return false;
    }

    public  static boolean  isNoPhaseDept(String deptForlderName){
        if(deptForlderName.equalsIgnoreCase("MPM")  || deptForlderName.equalsIgnoreCase("SIM-EI") ||deptForlderName.equalsIgnoreCase("SIM-SYS")){
            return true;
        }
        return false;
    }

}
