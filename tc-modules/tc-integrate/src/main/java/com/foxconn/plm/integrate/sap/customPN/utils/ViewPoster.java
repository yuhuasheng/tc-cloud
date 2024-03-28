package com.foxconn.plm.integrate.sap.customPN.utils;

import com.foxconn.plm.integrate.sap.customPN.domain.rp.CustomPartRp;
import com.foxconn.plm.integrate.sap.customPN.mapper.CustomPNMapper;
import com.foxconn.plm.integrate.sap.customPN.view.*;
import com.sap.conn.jco.*;

public class ViewPoster {


    private String commitPost(JCoDestination destination, JCoFunction function) throws Exception {

        function.execute(destination);
        JCoFunction functionCommit = destination.getRepository().getFunction("BAPI_TRANSACTION_COMMIT");
        functionCommit.execute(destination);

        JCoParameterList output = function.getExportParameterList();
        JCoStructure returnInfo = output.getStructure("RETURN");
        if ("S".equals(returnInfo.getValue("TYPE"))) {
            return "S";
        } else {
            String err = returnInfo.getString("MESSAGE");
            return err;
        }
    }

    private void setClientData(JCoParameterList paramList, String key, String value, String valueX) throws Exception {
        JCoStructure structureClient = paramList.getStructure("CLIENTDATA");
        JCoStructure structureClientX = paramList.getStructure("CLIENTDATAX");
        structureClient.setValue(key, value);
        structureClientX.setValue(key, valueX);
    }


    public String postBasicView(CustomPNMapper customPNMapper, JCoDestination destination, CustomPartRp applicationPartPojo, boolean is888) throws Exception {

        BasicView basicView = (BasicView) new ViewUtils().getView(customPNMapper, ViewConstants.basicView, applicationPartPojo);

        String strFunc = "BAPI_MATERIAL_SAVEDATA";
        JCoFunction function = destination.getRepository().getFunction(strFunc);

        JCoParameterList paramList = function.getImportParameterList();
        JCoParameterList tableList = function.getTableParameterList();
        JCoStructure structureHead = paramList.getStructure("HEADDATA");
        structureHead.setValue("MATERIAL", applicationPartPojo.getMaterialNumber());
        structureHead.setValue("MATL_TYPE", applicationPartPojo.getMaterialType());
        structureHead.setValue("IND_SECTOR", basicView.getIndustrySector());
        structureHead.setValue("BASIC_VIEW", "X");
        JCoTable tableDescr = tableList.getTable("MATERIALDESCRIPTION");
        tableDescr.appendRow();
        tableDescr.setValue("LANGU", "E");
        tableDescr.setValue("MATL_DESC", applicationPartPojo.getDescription());

        setClientData(paramList, "MATL_GROUP", applicationPartPojo.getMaterialGroup(), "X");
        setClientData(paramList, "OLD_MAT_NO", "", "X");

        if ("PC".equalsIgnoreCase(applicationPartPojo.getBaseUnit()) && is888) {
            setClientData(paramList, "BASE_UOM", "EA", "X");
        } else {
            setClientData(paramList, "BASE_UOM", applicationPartPojo.getBaseUnit(), "X");
        }
        setClientData(paramList, "UNIT_OF_WT_ISO", "G", "X");
        setClientData(paramList, "NET_WEIGHT", basicView.getNET_WEIGHT(), "X");
        setClientData(paramList, "UNIT_OF_WT", basicView.getWEIGHT_UNIT(), "X");

        JCoTable tableMeasure = tableList.getTable("UNITSOFMEASURE");
        JCoTable tableMeasureX = tableList.getTable("UNITSOFMEASUREX");
        tableMeasure.appendRow();
        tableMeasureX.appendRow();
        if ("PC".equalsIgnoreCase(applicationPartPojo.getBaseUnit()) && is888) {
            tableMeasure.setValue("ALT_UNIT", "EA");
            tableMeasureX.setValue("ALT_UNIT", "EA");
        } else {
            tableMeasure.setValue("ALT_UNIT", applicationPartPojo.getBaseUnit());
            tableMeasureX.setValue("ALT_UNIT", applicationPartPojo.getBaseUnit());
        }

        tableMeasure.setValue("GROSS_WT", basicView.getGROSS_WEIGHT());
        tableMeasureX.setValue("GROSS_WT", "X");

        tableMeasure.setValue("UNIT_OF_WT", basicView.getWEIGHT_UNIT());
        tableMeasureX.setValue("UNIT_OF_WT", "X");

        tableMeasure.setValue("UNIT_OF_WT_ISO", "G");
        tableMeasureX.setValue("UNIT_OF_WT_ISO", "X");

        return commitPost(destination, function);
    }


    public String postBasicViewCH(CustomPNMapper customPNMapper, JCoDestination destination, CustomPartRp applicationPartPojo, boolean is888) throws Exception {
        String zh = applicationPartPojo.getDescriptionZH();
        if (zh == null || "".equalsIgnoreCase(zh.trim())) {
            return "S";
        }
        BasicView basicView = (BasicView) new ViewUtils().getView(customPNMapper, ViewConstants.basicView, applicationPartPojo);

        String strFunc = "BAPI_MATERIAL_SAVEDATA";
        JCoFunction function = destination.getRepository().getFunction(strFunc);

        JCoParameterList paramList = function.getImportParameterList();
        JCoParameterList tableList = function.getTableParameterList();
        JCoStructure structureHead = paramList.getStructure("HEADDATA");
        structureHead.setValue("MATERIAL", applicationPartPojo.getMaterialNumber());
        structureHead.setValue("MATL_TYPE", applicationPartPojo.getMaterialType());
        structureHead.setValue("IND_SECTOR", basicView.getIndustrySector());
        structureHead.setValue("BASIC_VIEW", "X");
        JCoTable tableDescr = tableList.getTable("MATERIALDESCRIPTION");
        tableDescr.appendRow();
        tableDescr.setValue("LANGU", "M");
        String descr = applicationPartPojo.getDescriptionZH();
        String newStr = new String(descr.getBytes("UTF-8"), "UTF-8");
        tableDescr.setValue("MATL_DESC", newStr);

        setClientData(paramList, "MATL_GROUP", applicationPartPojo.getMaterialGroup(), "X");
        setClientData(paramList, "OLD_MAT_NO", "", "X");

        if ("PC".equalsIgnoreCase(applicationPartPojo.getBaseUnit()) && is888) {
            setClientData(paramList, "BASE_UOM", "EA", "X");
        } else {
            setClientData(paramList, "BASE_UOM", applicationPartPojo.getBaseUnit(), "X");
        }
        setClientData(paramList, "UNIT_OF_WT_ISO", "G", "X");
        setClientData(paramList, "NET_WEIGHT", basicView.getNET_WEIGHT(), "X");
        setClientData(paramList, "UNIT_OF_WT", basicView.getWEIGHT_UNIT(), "X");

        JCoTable tableMeasure = tableList.getTable("UNITSOFMEASURE");
        JCoTable tableMeasureX = tableList.getTable("UNITSOFMEASUREX");
        tableMeasure.appendRow();
        tableMeasureX.appendRow();
        if ("PC".equalsIgnoreCase(applicationPartPojo.getBaseUnit()) && is888) {
            tableMeasure.setValue("ALT_UNIT", "EA");
        } else {
            tableMeasure.setValue("ALT_UNIT", applicationPartPojo.getBaseUnit());
        }

        if ("PC".equalsIgnoreCase(applicationPartPojo.getBaseUnit()) && is888) {
            tableMeasureX.setValue("ALT_UNIT", "EA");
        } else {
            tableMeasureX.setValue("ALT_UNIT", applicationPartPojo.getBaseUnit());
        }


        tableMeasure.setValue("GROSS_WT", basicView.getGROSS_WEIGHT());
        tableMeasureX.setValue("GROSS_WT", "X");

        tableMeasure.setValue("UNIT_OF_WT", basicView.getWEIGHT_UNIT());
        tableMeasureX.setValue("UNIT_OF_WT", "X");

        tableMeasure.setValue("UNIT_OF_WT_ISO", "G");
        tableMeasureX.setValue("UNIT_OF_WT_ISO", "X");

        return commitPost(destination, function);
    }


    public String postMRPView(CustomPNMapper customPNMapper, JCoDestination destination, CustomPartRp applicationPartPojo) throws Exception {
        PurchasingView purchasingView = (PurchasingView) new ViewUtils().getView(customPNMapper, ViewConstants.clazzPurchasingView, applicationPartPojo);
        String plant = applicationPartPojo.getPlant();
        MRPView mprView = (MRPView) new ViewUtils().getView(customPNMapper, ViewConstants.clazzMRPView, applicationPartPojo);

        String strFunc = "BAPI_MATERIAL_SAVEDATA";
        JCoFunction function = destination.getRepository().getFunction(strFunc);
        JCoParameterList paramList = function.getImportParameterList();
        JCoParameterList tableList = function.getTableParameterList();

        JCoStructure structureHead = paramList.getStructure("HEADDATA");
        structureHead.setValue("MATERIAL", applicationPartPojo.getMaterialNumber());
        structureHead.setValue("MRP_VIEW", "X");
        setPlantData(paramList, "PLANT", applicationPartPojo.getPlant(), applicationPartPojo.getPlant());
        setPlantData(paramList, "MRP_GROUP", mprView.getMRP_GROUP(), "X");
        setPlantData(paramList, "MRP_TYPE", mprView.getMRP_TYPE(), "X");
        setPlantData(paramList, "MRP_CTRLER", mprView.getMRP_CONTROLLER(), "X");

        String sptype = "";
        String lotSize = mprView.getMRP_LOT_SIZE();
        String ps = applicationPartPojo.getPartSource();

        String mattype = applicationPartPojo.getMaterialType();
        if ("TB".equalsIgnoreCase(lotSize) && "F".equalsIgnoreCase(ps)) {
            sptype = "";
        } else if ("EX".equalsIgnoreCase(lotSize) && "E".equalsIgnoreCase(ps)) {
            if ("zmod".equalsIgnoreCase(applicationPartPojo.getMaterialType())) {
                sptype = "50";
            }
        }

        setPlantData(paramList, "PROC_TYPE", ps, "X");
        setPlantData(paramList, "LOTSIZEKEY", lotSize, "X");
        setPlantData(paramList, "SPPROCTYPE", sptype, "X");

        if (("AFEC".equalsIgnoreCase(plant)) || ("AFLC".equalsIgnoreCase(plant))) {
            setPlantData(paramList, "PUR_GROUP", "W01,W02,W03,W04,W05,W06,W07,W08,W09", "X");
        } else {
            setPlantData(paramList, "PUR_GROUP", purchasingView.getPURCHASING_GROUP(), "X");//"PAX"
        }

        setPlantData(paramList, "ROUND_VAL", mprView.getMRP_ROUNDING_VALUE(), "X");
        setPlantData(paramList, "SAFETY_STK", mprView.getMRP_SAFETY_STOCK(), "X");
        setPlantData(paramList, "REORDER_PT", mprView.getMRP_RE_ORDER_POINT(), "X");
        setPlantData(paramList, "GR_PR_TIME", mprView.getMRP_GR_PROCESSING_DAY(), "X");
        setPlantData(paramList, "SM_KEY", mprView.getMRP_SCHED_MARGIN_KEY(), "X");
        setPlantData(paramList, "SLOC_EXPRC", mprView.getMRP_STORAGE_LOC_FOR_EP(), "X");
        setPlantData(paramList, "ALT_BOM_ID", mprView.getMRP_SELECTION_METHOD(), "X");
        setPlantData(paramList, "REP_MANUF", mprView.getMRP_REPETITIVE_MFG(), "X");
        setPlantData(paramList, "REPMANPROF", mprView.getMRP_REM_PROFILE(), "X");
        setPlantData(paramList, "MAXLOTSIZE", mprView.getMRP_MAX_LOT_SIZE(), "X");
        setPlantData(paramList, "BACKFLUSH", mprView.getMRP_BACKFLUSH(), "X");
        setPlantData(paramList, "INHSEPRODT", mprView.getMRP_INHOUSE_PRODUCTION(), "X");
        setPlantData(paramList, "PLAN_STRGP", mprView.getMRP_STRATEGY_GROUP(), "X");
        setPlantData(paramList, "MIXED_MRP", mprView.getMRP_MIXED_MRP(), "X");
        setPlantData(paramList, "AVAILCHECK", mprView.getMRP_AVAILABILITY_CHECK(), "X");
        setPlantData(paramList, "PRODPROF", mprView.getMRP_PROD_SCHED_PROFILE(), "X");
        setPlantData(paramList, "DEP_REQ_ID", mprView.getMRP_INDIVIDUAL_COLL(), "X");
        setPlantData(paramList, "BULK_MAT", mprView.getMRP_BULK_MATERIAL(), "X");
        setPlantData(paramList, "PLND_DELRY", mprView.getMRP_PLANNED_DELV_TIME(), "X");


        if (("ZROH".equalsIgnoreCase(applicationPartPojo.getMaterialType())) || ("ZWAR".equalsIgnoreCase(applicationPartPojo.getMaterialType()))) {


            if (("CHMM".equalsIgnoreCase(plant)) || ("CHMN".equalsIgnoreCase(plant))) {
                setPlantData(paramList, "ISS_ST_LOC", "BS11", "X");
            } else if ("AFLC".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BN10,BN11,NN10,NN11", "X");
            } else if ("AFEC".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BM10,BM11,NM10,NM11", "X");
            } else if ("CHMP".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BF11", "X");
            } else if ("CHMQ".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BH11", "X");
            } else {
                setPlantData(paramList, "ISS_ST_LOC", mprView.getMRP_PROD_STORAGE_LOC(), "X");
            }

        } else if ("ZFRT".equalsIgnoreCase(applicationPartPojo.getMaterialType())) {

            if (("CHMM".equalsIgnoreCase(plant)) || ("CHMN".equalsIgnoreCase(plant))) {
                setPlantData(paramList, "ISS_ST_LOC", "BS31", "X");
            } else if ("AFLC".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BN31", "X");
            } else if ("AFEC".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BM31", "X");
            } else if ("PFEA".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BB31", "X");
            } else if ("PFLA".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BA31", "X");
            } else if ("CHMP".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BF31", "X");
            } else if ("CHMQ".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BH31", "X");
            } else {
                setPlantData(paramList, "ISS_ST_LOC", mprView.getMRP_PROD_STORAGE_LOC(), "X");
            }
        } else if (("ZHLB".equalsIgnoreCase(applicationPartPojo.getMaterialType())) || ("ZMOD".equalsIgnoreCase(applicationPartPojo.getMaterialType()))) {

            if (("CHMM".equalsIgnoreCase(plant)) || ("CHMN".equalsIgnoreCase(plant))) {
                setPlantData(paramList, "ISS_ST_LOC", "BS21", "X");
            } else if ("AFLC".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BN21", "X");
            } else if ("AFEC".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BM21", "X");
            } else if ("CHMP".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BF21", "X");
            } else if ("CHMQ".equalsIgnoreCase(plant)) {
                setPlantData(paramList, "ISS_ST_LOC", "BH21", "X");
            } else {
                setPlantData(paramList, "ISS_ST_LOC", mprView.getMRP_PROD_STORAGE_LOC(), "X");
            }
        } else {
            setPlantData(paramList, "ISS_ST_LOC", mprView.getMRP_PROD_STORAGE_LOC(), "X");
        }

        return commitPost(destination, function);

    }

    private void setPlantData(JCoParameterList paramList, String key, String value, String valueX) throws Exception {
        JCoStructure structurePlant = paramList.getStructure("PLANTDATA");
        JCoStructure structurePlantX = paramList.getStructure("PLANTDATAX");
        structurePlant.setValue(key, value);
        structurePlantX.setValue(key, valueX);
    }

    public String postPurchasingAndCostingView(CustomPNMapper customPNMapper, JCoDestination destination, CustomPartRp applicationPartPojo) throws Exception {
        CostingView costingView = (CostingView) new ViewUtils().getView(customPNMapper, ViewConstants.clazzCostingView, applicationPartPojo);
        PurchasingView purchasingView = (PurchasingView) new ViewUtils().getView(customPNMapper, ViewConstants.clazzPurchasingView, applicationPartPojo);

        String strFunc = "BAPI_MATERIAL_SAVEDATA";
        JCoFunction function = destination.getRepository().getFunction(
                strFunc);
        JCoParameterList paramList = function.getImportParameterList();
        JCoParameterList tableList = function.getTableParameterList();

        JCoStructure structureHead = paramList.getStructure("HEADDATA");
        structureHead.setValue("MATERIAL", applicationPartPojo.getMaterialNumber());
        structureHead.setValue("PURCHASE_VIEW", "X");

        setPlantData(paramList, "PLANT", applicationPartPojo.getPlant(), applicationPartPojo.getPlant());
        setPlantData(paramList, "PROFIT_CTR", costingView.getCENTER_CODE(), "X");

        String p = applicationPartPojo.getPlant();
        if (("CHMD".equalsIgnoreCase(p)) || ("CHMK".equalsIgnoreCase(p)) || ("SSTF".equalsIgnoreCase(p)) ||
                ("JLMG".equalsIgnoreCase(p)) || ("CHKC".equalsIgnoreCase(p)) || ("AHME".equalsIgnoreCase(p)) ||
                ("AHMI".equalsIgnoreCase(p)) || ("JLME".equalsIgnoreCase(p)) || ("JLMP".equalsIgnoreCase(p))) {
            setPlantData(paramList, "SOURCELIST", "", "X");
        } else {
            setPlantData(paramList, "SOURCELIST", purchasingView.getPURCHASING_SOURCELIST(), "X");//X
        }

        if (("AFEC".equalsIgnoreCase(p)) || ("AFLC".equalsIgnoreCase(p))) {
            setPlantData(paramList, "PUR_GROUP", "W01,W02,W03,W04,W05,W06,W07,W08,W09", "X");
        } else {
            setPlantData(paramList, "PUR_GROUP", purchasingView.getPURCHASING_GROUP(), "X");//"PAX"
        }

        JCoStructure structureClient = paramList.getStructure("CLIENTDATA");
        JCoStructure structureClientX = paramList.getStructure("CLIENTDATAX");
        structureClient.setValue("MANUF_PROF", purchasingView.getPURCHASING_MANU_CONTROL_KEY());
        structureClientX.setValue("MANUF_PROF", "X");

        return commitPost(destination, function);
    }


    public String postSDViewAndCostingView(CustomPNMapper customPNMapper, JCoDestination destination, CustomPartRp applicationPartPojo) throws Exception {
        CostingView costingView = (CostingView) new ViewUtils().getView(customPNMapper, ViewConstants.clazzCostingView, applicationPartPojo);
        SDView sdview = (SDView) new ViewUtils().getView(customPNMapper, ViewConstants.clazzSDView, applicationPartPojo);
        String strFunc = "BAPI_MATERIAL_SAVEDATA";
        JCoFunction function = destination.getRepository().getFunction(
                strFunc);
        JCoParameterList paramList = function.getImportParameterList();
        JCoParameterList tableList = function.getTableParameterList();

        JCoStructure structureHead = paramList.getStructure("HEADDATA");
        structureHead.setValue("MATERIAL", applicationPartPojo.getMaterialNumber());
        structureHead.setValue("SALES_VIEW", "X");

        JCoStructure structureClient = paramList.getStructure("CLIENTDATA");
        JCoStructure structureClientX = paramList.getStructure("CLIENTDATAX");
        structureClient.setValue("TRANS_GRP", sdview.getSD_TRANSPORTATION_GROUP());//P001
        structureClientX.setValue("TRANS_GRP", "X");
        structureClient.setValue("MAT_GRP_SM", sdview.getSD_MATL_GRP_PACK_MATLS());
        structureClientX.setValue("MAT_GRP_SM", "X");

        setPlantData(paramList, "PLANT", applicationPartPojo.getPlant(), applicationPartPojo.getPlant());
        setPlantData(paramList, "LOADINGGRP", sdview.getSD_LOADINIG_GROUP(), "X");//"F001"
        setPlantData(paramList, "AVAILCHECK", sdview.getSD_AVAILABILITY_CHECK(), "X");//"Z2"

        JCoStructure structureSals = paramList.getStructure("SALESDATA");
        JCoStructure structureSalsX = paramList.getStructure("SALESDATAX");
        structureSals.setValue("SALES_ORG", sdview.getSD_SALES_ORGANIZATION());//"CQB2"
        structureSalsX.setValue("SALES_ORG", sdview.getSD_SALES_ORGANIZATION());//"CQB2"
        structureSals.setValue("ITEM_CAT", sdview.getSD_ITEM_CATEGORY_GROUP());//"NORM"
        structureSalsX.setValue("ITEM_CAT", "X");
        structureSals.setValue("DISTR_CHAN", sdview.getSD_DISTRIBUTION_CHANNEL());//"DC"
        structureSalsX.setValue("DISTR_CHAN", sdview.getSD_DISTRIBUTION_CHANNEL());//"DC"
        structureSals.setValue("MATL_STATS", sdview.getSD_STATISTICS_GROUP());//"1"
        structureSalsX.setValue("MATL_STATS", "X");
        String plant = applicationPartPojo.getPlant();
        if (("DCA1".equalsIgnoreCase(plant)) || ("DFL1".equalsIgnoreCase(plant)) || ("DIL1".equalsIgnoreCase(plant)) ||
                ("DTN1".equalsIgnoreCase(plant)) || ("DTX1".equalsIgnoreCase(plant)) || ("DTX2".equalsIgnoreCase(plant)) ||
                ("DTX3".equalsIgnoreCase(plant)) || ("HCA1".equalsIgnoreCase(plant)) || ("HFL1".equalsIgnoreCase(plant)) ||
                ("HIN1".equalsIgnoreCase(plant)) || ("HTX1".equalsIgnoreCase(plant)) || ("LNC1".equalsIgnoreCase(plant))) {
            structureSals.setValue("ACCT_ASSGT", "04");
            structureSalsX.setValue("ACCT_ASSGT", "X");
        } else {
            structureSals.setValue("ACCT_ASSGT", sdview.getSD_ACCOUNT_ASSIGNMENT_GROUP());//"02"
            structureSalsX.setValue("ACCT_ASSGT", "X");
        }


        if (("CHMD".equalsIgnoreCase(plant)) || ("CHME".equalsIgnoreCase(plant))) {
            addTAXRecord(tableList, "AS", "MWST");
        } else if (("CHMA".equalsIgnoreCase(plant)) || ("CHMB".equalsIgnoreCase(plant)) ||
                ("CHMC".equalsIgnoreCase(plant)) || ("CHMK".equalsIgnoreCase(plant)) ||
                ("CHKA".equalsIgnoreCase(plant)) || ("CHKB".equalsIgnoreCase(plant)) ||
                ("CHKC".equalsIgnoreCase(plant)) || ("AHMA".equalsIgnoreCase(plant)) ||
                ("AHMB".equalsIgnoreCase(plant)) || ("AHMC".equalsIgnoreCase(plant)) ||
                ("AHME".equalsIgnoreCase(plant)) ||
                ("PFEA".equalsIgnoreCase(plant)) || ("PFLA".equalsIgnoreCase(plant)) ||
                ("CHMM".equalsIgnoreCase(plant)) || ("CHMN".equalsIgnoreCase(plant)) ||
                ("CHMP".equalsIgnoreCase(plant)) || ("CHMQ".equalsIgnoreCase(plant)) ||
                ("CHMS".equalsIgnoreCase(plant)) || ("CHMU".equalsIgnoreCase(plant)) ||
                ("FKY1".equalsIgnoreCase(plant)) || ("FGA1".equalsIgnoreCase(plant)) || ("FPA1".equalsIgnoreCase(plant)) || ("FCA1".equalsIgnoreCase(plant)) || ("FIL1".equalsIgnoreCase(plant)) || ("CQSA".equalsIgnoreCase(plant)) ||
                ("PHMC".equalsIgnoreCase(plant)) || ("AHMT".equalsIgnoreCase(plant)) ||
                ("CHMF".equalsIgnoreCase(plant)) || ("CHMY".equalsIgnoreCase(plant)) ||
                ("CHMY".equalsIgnoreCase(plant)) || ("FHA1".equalsIgnoreCase(plant)) ||
                ("CHJM".equalsIgnoreCase(plant)) || ("CHMR".equalsIgnoreCase(plant)) ||
                ("JLMF".equalsIgnoreCase(plant)) || ("JLMD".equalsIgnoreCase(plant)) ||
                ("CHMZ".equalsIgnoreCase(plant))) {
            addTAXRecord(tableList, "CN", "MWST");
        } else if (("ACDC".equalsIgnoreCase(plant)) || ("AHMK".equalsIgnoreCase(plant))) {
            addTAXRecord(tableList, "IE", "MWST");
        } else if ("LF48".equalsIgnoreCase(plant)) {
            addTAXRecord(tableList, "JP", "MWST");
        } else if ("SSTF".equalsIgnoreCase(plant)) {
            addTAXRecord(tableList, "SG", "MWST");
            addTAXRecord(tableList, "TR", "MWST");
        } else if ("AHMI".equalsIgnoreCase(plant)) {
            addTAXRecord(tableList, "AS", "MWST");
            addTAXRecord(tableList, "CN", "MWST");
        } else if (("DCA1".equalsIgnoreCase(plant)) || ("DFL1".equalsIgnoreCase(plant)) ||
                ("DIL1".equalsIgnoreCase(plant)) || ("DTN1".equalsIgnoreCase(plant)) ||
                ("DTX1".equalsIgnoreCase(plant)) || ("DTX2".equalsIgnoreCase(plant)) ||
                ("DTX3".equalsIgnoreCase(plant)) || ("HCA1".equalsIgnoreCase(plant)) ||
                ("HFL1".equalsIgnoreCase(plant)) || ("HIN1".equalsIgnoreCase(plant)) ||
                ("HTX1".equalsIgnoreCase(plant)) || ("LNC1".equalsIgnoreCase(plant))) {
            addTAXRecord(tableList, "US", "UTXJ");
            addTAXRecord(tableList, "US", "UTX2");
            addTAXRecord(tableList, "US", "UTX3");
        } else if (("AIDB".equalsIgnoreCase(plant)) ||
                ("AFEB".equalsIgnoreCase(plant)) ||
                ("AFLA".equalsIgnoreCase(plant)) ||
                ("AFEC".equalsIgnoreCase(plant)) || ("AFLC".equalsIgnoreCase(plant))) {
            addTAXRecord(tableList, "CN", "MWST");
            addTAXRecord(tableList, "TW", "MWST");
        }

        setPlantData(paramList, "PLANT", plant, plant);
        setPlantData(paramList, "PROFIT_CTR", costingView.getCENTER_CODE(), "X");

        return commitPost(destination, function);

    }

    private void addTAXRecord(JCoParameterList tableList, String country, String TAXType) {
        JCoTable tableTaxClassification = tableList.getTable("TAXCLASSIFICATIONS");
        tableTaxClassification.appendRow();
        tableTaxClassification.setValue("DEPCOUNTRY", country);
        tableTaxClassification.setValue("TAX_TYPE_1", TAXType);
        tableTaxClassification.setValue("TAXCLASS_1", "0");
    }


    public String postAccountingView(CustomPNMapper customPNMapper, JCoDestination destination, CustomPartRp applicationPartPojo) throws Exception {
        AccountingView accountingView = (AccountingView) new ViewUtils().getView(customPNMapper, ViewConstants.clazzAccountingView, applicationPartPojo);

        String strFunc = "BAPI_MATERIAL_SAVEDATA";
        JCoFunction function = destination.getRepository().getFunction(
                strFunc);
        JCoParameterList paramList = function.getImportParameterList();
        JCoParameterList tableList = function.getTableParameterList();

        JCoStructure structureHead = paramList.getStructure("HEADDATA");
        structureHead.setValue("MATERIAL", applicationPartPojo.getMaterialNumber());
        structureHead.setValue("ACCOUNT_VIEW", "X");
        structureHead.setValue("COST_VIEW", "X");


        setPlantData(paramList, "PLANT", applicationPartPojo.getPlant(), applicationPartPojo.getPlant());
        setPlantData(paramList, "PROFIT_CTR", accountingView.getACCOUNTION_PROFIT_CENTER(), "X");


        JCoStructure structValuation = paramList.getStructure("VALUATIONDATA");
        JCoStructure structValuationX = paramList.getStructure("VALUATIONDATAX");

        structValuation.setValue("PRICE_CTRL", accountingView.getACCOUNTING_PRICE_CONTROL());
        structValuationX.setValue("PRICE_CTRL", "X");

        structValuation.setValue("VAL_CLASS", accountingView.getACCOUNTING_VALUATION_CLASS());
        structValuationX.setValue("VAL_CLASS", "X");

        structValuation.setValue("ORIG_MAT", accountingView.getACCOUNTION_ORIGIN_GROUP());
        structValuationX.setValue("ORIG_MAT", "X");

        structValuation.setValue("PRICE_UNIT", accountingView.getACCOUNTION_PRICE_UNIT());
        structValuationX.setValue("PRICE_UNIT", "X");


        structValuation.setValue("VAL_AREA", applicationPartPojo.getPlant());
        structValuationX.setValue("VAL_AREA", applicationPartPojo.getPlant());

        String plant = applicationPartPojo.getPlant();

        if (("ACDC".equalsIgnoreCase(plant)) || ("AHMK".equalsIgnoreCase(plant))) {
            structValuation.setValue("STD_PRICE", accountingView.getACCOUNTION_STANDARD_PRICE());
            structValuationX.setValue("STD_PRICE", "X");
            System.out.println("Send Accounting View Infor:Plant:" + plant + ",Params:" + accountingView.getACCOUNTION_STANDARD_PRICE() + ",STD_PRICE");
        } else {
            structValuation.setValue("MOVING_PR", accountingView.getACCOUNTING_MOV_AVG_PRICE());
            structValuationX.setValue("MOVING_PR", "X");
        }

        return commitPost(destination, function);

    }

}
