package com.foxconn.plm.spas.service;

public interface SynSpasDBService {
    void addUserRoleData() throws Exception;

    void addUserData() throws Exception;

    void addRoleData() throws Exception;

    void addOrganizationData() throws Exception;

    void addDeptGroupData() throws Exception;

    void addProjectSeriesData() throws Exception;

    void addProjectScheduleData() throws Exception;

    void addProjectPersonData() throws Exception;

    void addProjectAttributeData() throws Exception;

    void addProductLinePhaseData() throws Exception;

    void addProductLineData() throws Exception;

    void addPlatformFoundData() throws Exception;

    void addCustomerData() throws Exception;

    void addCusAttributeCategoryData() throws Exception;

    void addCusAttributeData() throws Exception;

    void addStiTeamRosterData() throws Exception;

    void addManpowerStandardData() throws Exception;
    public void updateManpowerStandardData() throws Exception;

    void addFunctionData() throws Exception;


    void addRoutingData() throws Exception;
    void updateRoutingData() throws Exception;


    void test() throws Exception;
}
