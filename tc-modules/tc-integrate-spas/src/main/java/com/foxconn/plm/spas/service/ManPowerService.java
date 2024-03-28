package com.foxconn.plm.spas.service;

import com.foxconn.plm.spas.bean.ManpowerActionInfo;
import com.foxconn.plm.spas.bean.ManpowerInfo;
import com.foxconn.plm.spas.bean.TempInfo;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.teamcenter.soa.client.model.strong.Folder;

import java.util.List;

public interface ManPowerService {


    public List<ManpowerInfo> getManPowers(String projectId) throws Exception ;
    public  List<TempInfo> updatePrtFolder(String spasProjectId, boolean isSyned, Folder projectFolder, ManpowerActionInfo manpowerActionInfos, String bu , TCSOAServiceFactory tCSOAServiceFactory,String snapId) throws Exception;
    public  List<TempInfo>  updateMNTFolder(String spasProjectId,boolean isSyned, Folder projectFolder, ManpowerActionInfo manpowerActionInfos,String bu, String platformLevel, TCSOAServiceFactory tCSOAServiceFactory,String snapId) throws Exception ;
    public   List<TempInfo> updateDTFolder(String spasProjectId, boolean isSyned, Folder projectFolder, String customerName, String productLine, ManpowerActionInfo manpowerActionInfos, String bu, TCSOAServiceFactory tCSOAServiceFactory,String snapId) throws Exception ;

    public   List<TempInfo> updateSHFolder(String spasProjectId, boolean isSyned, Folder projectFolder, String customerName, ManpowerActionInfo manpowerActionInfos, String bu, TCSOAServiceFactory tCSOAServiceFactory,String snapId) throws Exception ;



}
