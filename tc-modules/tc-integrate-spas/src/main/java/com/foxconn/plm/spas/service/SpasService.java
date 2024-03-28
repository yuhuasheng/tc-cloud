package com.foxconn.plm.spas.service;

import com.foxconn.plm.spas.bean.ManpowerInfo;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.teamcenter.soa.client.model.strong.Folder;

import java.util.List;

public interface SpasService {


    public  void createDTFolder(Folder projectFolder, String customerName, String productLine, List<ManpowerInfo> manpowerInfos, List<String> phaseList, TCSOAServiceFactory tCSOAServiceFactory) throws Exception ;
    public  void createMNTFolder(Folder projectFolder, List<ManpowerInfo> manpowerInfos, String platformLevel, TCSOAServiceFactory tCSOAServiceFactory) throws Exception ;
    public  void createPrtFolder(Folder projectFolder, List<ManpowerInfo> manpowerInfos, TCSOAServiceFactory tCSOAServiceFactory) throws Exception ;

    public  void createSHFolder(Folder projectFolder, List<ManpowerInfo> manpowerInfos, TCSOAServiceFactory tCSOAServiceFactory,String customerName) throws Exception ;


    }
