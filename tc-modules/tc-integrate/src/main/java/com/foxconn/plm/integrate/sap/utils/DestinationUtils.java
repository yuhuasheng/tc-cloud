package com.foxconn.plm.integrate.sap.utils;

import com.foxconn.plm.integrate.sap.customPN.utils.ConnectPoolUtils;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;

public class DestinationUtils {

    public static final String[] plant801 = {"CHMB", "CHMC", "CHMD", "CHMK", "CHJM", "AFLC", "AFEC", "JLMC", "JLMB", "WLLA", "AHLX","CHPA"};

    public static final String[] plant888 = {"DCA1", "DFL1", "DIL1", "DTN1", "DTX1", "DTX2", "DTX3", "HCA1", "HFL1", "HIN1", "HTX1", "LNC1", "CQSA", "CHMS", "CHMU", "LF48"};

    public static final String[] plant868 = {"ACDC", "AHMK"};

    public static JCoDestination getJCoDestination(String plant, JCoDestination j801, JCoDestination j888, JCoDestination j868) {
        for (String p : plant888) {
            if (p.equalsIgnoreCase(plant)) {
                return j888;
            }
        }

        for (String p : plant801) {
            if (p.equalsIgnoreCase(plant)) {
                return j801;
            }
        }
        for (String p : plant868) {
            if (p.equalsIgnoreCase(plant)) {
                return j868;
            }
        }

        return null;
    }


    public static JCoDestination getJCoDestination(String plant) throws Exception {
        for (String p : plant888) {
            if (p.equalsIgnoreCase(plant)) {
                return JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED_888);
            }
        }

        for (String p : plant801) {
            if (p.equalsIgnoreCase(plant)) {
                return JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED);
            }
        }
        for (String p : plant868) {
            if (p.equalsIgnoreCase(plant)) {
                return JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED_868);
            }
        }

        return null;
    }


    public static boolean is888(String plant) {
        for (String p : plant888) {
            if (p.equalsIgnoreCase(plant)) {
                return true;
            }
        }
        return false;
    }

}
