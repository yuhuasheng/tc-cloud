package com.foxconn.plm.utils.tc;

import com.teamcenter.services.strong.core.ProjectLevelSecurityService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.TC_Project;

public class ProjectUtil {

    public static ServiceData assignedProject(ProjectLevelSecurityService projectLevelSecurityService, ModelObject folder, TC_Project project) {
        com.teamcenter.services.strong.core._2007_09.ProjectLevelSecurity.AssignedOrRemovedObjects assignedOrRemovedObjects = new com.teamcenter.services.strong.core._2007_09.ProjectLevelSecurity.AssignedOrRemovedObjects();
        assignedOrRemovedObjects.objectToAssign = new ModelObject[]{folder};
        //assignedOrRemovedObjects.objectToRemove = null;
        assignedOrRemovedObjects.projects = new TC_Project[]{project};

        com.teamcenter.services.strong.core._2007_09.ProjectLevelSecurity.AssignedOrRemovedObjects[] aassignedorremovedobjects = new com.teamcenter.services.strong.core._2007_09.ProjectLevelSecurity.AssignedOrRemovedObjects[1];
        aassignedorremovedobjects[0] = assignedOrRemovedObjects;

        return projectLevelSecurityService.assignOrRemoveObjects(aassignedorremovedobjects);
    }

}
