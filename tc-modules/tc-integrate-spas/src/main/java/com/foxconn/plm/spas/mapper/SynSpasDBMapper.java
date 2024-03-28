package com.foxconn.plm.spas.mapper;

import com.foxconn.plm.spas.bean.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface SynSpasDBMapper {

    void deleteUserRole(SpasUserRole userRole) throws Exception;

    void saveUserRole(SpasUserRole userRole) throws Exception;

    void deleteUser(SpasUser user) throws Exception;

    void saveUser(SpasUser user) throws Exception;

    void deleteRole(SpasRole role) throws Exception;

    void saveRole(SpasRole role) throws Exception;

    void deleteOrganization(SpasOrganization organization) throws Exception;

    void saveOrganization(SpasOrganization organization) throws Exception;

    void deleteDeptGroup(SpasDeptGroup deptGroup) throws Exception;

    void saveDeptGroup(SpasDeptGroup deptGroup) throws Exception;

    void deleteProjectSeries(SpasProjectSeries projectSeries) throws Exception;

    void saveProjectSeries(SpasProjectSeries projectSeries) throws Exception;

    void deleteProjectSchedule(SpasProjectSchedule projectSchedule) throws Exception;

    void saveProjectSchedule(SpasProjectSchedule projectSchedule) throws Exception;

    void deleteProjectPerson(SpasProjectPerson projectPerson) throws Exception;

    void saveProjectPerson(SpasProjectPerson projectPerson) throws Exception;

    void deleteProjectAttribute(SpasProjectAttribute projectAttribute) throws Exception;

    void saveProjectAttribute(SpasProjectAttribute projectAttribute) throws Exception;

    void deleteProductLinePhase(SpasProductLinePhase productLinePhase) throws Exception;

    void saveProductLinePhase(SpasProductLinePhase productLinePhase) throws Exception;

    void deleteProductLine(SpasProductLine productLine) throws Exception;

    void saveProductLine(SpasProductLine productLine) throws Exception;

    void deletePlatformFound(SpasPlatformFound platformFound) throws Exception;

    void savePlatformFound(SpasPlatformFound platformFound) throws Exception;

    void deleteCustomer(SpasCustomer spasCustomer) throws Exception;

    void saveCustomer(SpasCustomer spasCustomer) throws Exception;

    void deleteCusAttributeCategory(SpasCusAttributeCategory cusAttributeCategory) throws Exception;

    void saveCusAttributeCategory(SpasCusAttributeCategory cusAttributeCategory) throws Exception;

    void deleteCusAttribute(SpasCusAttribute cusAttribute) throws Exception;

    void saveCusAttribute(SpasCusAttribute cusAttribute) throws Exception;

    void deleteStiTeamRoster(SpasStiTeamRoster stiTeamRoster) throws Exception;

    void saveStiTeamRoster(SpasStiTeamRoster stiTeamRoster) throws Exception;

    void deleteManpowerStandard(SpasManpowerStandard manpowerStandard) throws Exception;

    void saveManpowerStandard(SpasManpowerStandard manpowerStandard) throws Exception;

    void updateManpowerStandard(SpasManpowerStandard manpowerStandard) throws Exception;

    void deleteFunction(SpasFunction function) throws Exception;

    void saveFunction(SpasFunction function) throws Exception;

    PlatformFound getSpasProject(String projectId) throws Exception;
    PlatformFound getSpasProject2(String projectId) throws Exception;

    Integer getHandleState(String projectId) throws Exception;

//
    List<FolderInfo> getDeptFolders(String projectId) throws Exception;
    List<FolderInfo> getChildFolders(Integer folderId) throws Exception;
    Integer getDocumentCnt(Integer folderId) throws Exception;
    void  deleteFolder(Integer folderId) throws Exception;
    void  deleteStru(Integer struId) throws Exception;
    public   void insertFolder(FolderInfo folderInfo)throws Exception;
    public void insertFolderStruct(FolderInfo folderInfo)throws Exception;

    public Integer  getNoHandleCnt(String projectId) throws Exception;


    void deleteRouting(SpasProjectRouting function) throws Exception;

    void upRouting(SpasProjectRouting function) throws Exception;

    void saveRouting(SpasProjectRouting function) throws Exception;

    STIProject getProjectInfo(String projectId) throws Exception;


    void addSpasActionHis(SpasActionHis spasActionHis) throws Exception ;


    List<Integer>getManpowerDiff(String srartDate)throws Exception ;

    List<ManpowerRawInfo> getManpowerAction(Integer projectId) throws Exception;
    void addActionDate(@Param("snapId") String snapId, @Param("projectId")Integer projectId, @Param("createDate")String createDate, @Param("updateDate")String updateDate)throws Exception;
    void addSnap(ManpowerRawInfo ManpowerRawInfo)throws Exception;

   void  addSnapHis(@Param("snapId") String snapId, @Param("projectId")Integer projectId, @Param("msg")String msg)throws Exception;


   Integer getManpower( @Param("projectId")String projectId,@Param("functionName")String functionName,@Param("phaseName")String phaseName) throws Exception;
}
