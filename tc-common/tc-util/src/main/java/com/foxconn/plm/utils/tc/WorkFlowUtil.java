package com.foxconn.plm.utils.tc;

import cn.hutool.core.util.StrUtil;
import com.foxconn.plm.entity.constants.TCEPMTaskConstant;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.services.strong.workflow._2013_05.Workflow;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.EPMTaskTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2023/3/13 17:14
 * @Version 1.0
 */
public class WorkFlowUtil {


    /**
     * 获取所有的流程模板
     * @param workflowService 流程服务类
     * @param dmService
     * @return
     * @throws Exception
     */
    public static Map<EPMTaskTemplate, String> getAllWorkflowTemplates(WorkflowService workflowService, DataManagementService dmService) throws Exception {
        Map<EPMTaskTemplate, String> retMap = new LinkedHashMap<>();
        Workflow.GetWorkflowTemplatesInputInfo[] getWorkflowTemplatesInputInfos = new Workflow.GetWorkflowTemplatesInputInfo[1];
        Workflow.GetWorkflowTemplatesInputInfo getWorkflowTemplatesInputInfo = new Workflow.GetWorkflowTemplatesInputInfo();
        getWorkflowTemplatesInputInfo.getFiltered = false;
        getWorkflowTemplatesInputInfo.includeUnderConstruction = false;
        getWorkflowTemplatesInputInfos[0] = getWorkflowTemplatesInputInfo;
        Workflow.GetWorkflowTemplatesResponse response = workflowService.getWorkflowTemplates(getWorkflowTemplatesInputInfos);
        String result = TCUtils.getErrorMsg(response.serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }

        Workflow.GetWorkflowTemplatesOutput[] templatesOutput = response.templatesOutput;
        for (Workflow.GetWorkflowTemplatesOutput output : templatesOutput) {
            EPMTaskTemplate[] workflowTemplates = output.workflowTemplates;
            for (EPMTaskTemplate task : workflowTemplates) {
                String templateName = TCUtils.getPropStr(dmService, task, TCEPMTaskConstant.PROPERTY_TEMPLATE_NAME);
                retMap.put(task, templateName);
            }
        }
        return retMap;
    }


    public static void createNewProcess(WorkflowService wfService, String workflowName, String
            processTemplate, ModelObject[] objects) throws ServiceException {
        boolean startImmediately = true;
        String observerKey = "";
        String name = workflowName;
        String subject = "";
        String description = "";

        com.teamcenter.services.strong.workflow._2008_06.Workflow.ContextData contextData = new com.teamcenter.services.strong.workflow._2008_06.Workflow.ContextData();
        contextData.attachmentCount = objects.length;
        String[] attachments = new String[objects.length];
        int[] attachmentTypes = new int[objects.length];
        for (int i = 0; i < objects.length; i++) {
            attachments[i] = objects[i].getUid();
            attachmentTypes[i] = 1;
        }
        contextData.attachments = attachments;
        contextData.attachmentTypes = attachmentTypes;
        contextData.processTemplate = processTemplate;
        contextData.subscribeToEvents = false;
        contextData.subscriptionEventCount = 0;

        com.teamcenter.services.strong.workflow._2008_06.Workflow.InstanceInfo instanceInfo = wfService.createInstance(startImmediately, observerKey, name, subject, description, contextData);
        ServiceData serviceData = instanceInfo.serviceData;
        if (serviceData.sizeOfPartialErrors() > 0) {
            throw new ServiceException(serviceData.getPartialError(0).toString());
        }
    }
}
