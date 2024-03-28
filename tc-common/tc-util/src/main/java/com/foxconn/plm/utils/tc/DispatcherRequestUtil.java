package com.foxconn.plm.utils.tc;

import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DispatcherManagementService;
import com.teamcenter.services.strong.core._2008_06.DispatcherManagement;
import com.teamcenter.soa.client.model.ModelObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2023/3/7 16:57
 * @Version 1.0
 */
public class DispatcherRequestUtil {

    public static boolean sendDispatcherRequest(DispatcherManagementService dispatcherService, String providerName, String serviceName, ModelObject[] primaryObjects, ModelObject[] secondaryObjects, int priority, int interval, Map<String, String> paramsMap) throws ServiceException {
        DispatcherManagement.CreateDispatcherRequestArgs[] dispatcherRequestArgs = new DispatcherManagement.CreateDispatcherRequestArgs[1];
        DispatcherManagement.CreateDispatcherRequestArgs arg = new DispatcherManagement.CreateDispatcherRequestArgs();
        arg.providerName = providerName;
        arg.serviceName = serviceName;
        arg.primaryObjects = primaryObjects;
        arg.secondaryObjects = secondaryObjects;
        arg.priority = priority;
        arg.interval = interval;
        DispatcherManagement.KeyValueArguments[] keyValueArgs = new DispatcherManagement.KeyValueArguments[paramsMap.size()];
        Iterator<Map.Entry<String, String>> iterator = paramsMap.entrySet().iterator();
        int index = 0;
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            keyValueArgs[index] = new DispatcherManagement.KeyValueArguments();
            keyValueArgs[index].key = entry.getKey();
            keyValueArgs[index].value = entry.getValue();
            index++;
        }
        arg.keyValueArgs = keyValueArgs;
        dispatcherRequestArgs[0] = arg;
        DispatcherManagement.CreateDispatcherRequestResponse response = dispatcherService.createDispatcherRequest(dispatcherRequestArgs);
        int errorSize = response.svcData.sizeOfPartialErrors();
        if (errorSize > 0) {
            for (int i = 0; i < errorSize; i++) {
                System.out.println("【ERROR】send dispatcher request response error info : " + Arrays.toString(response.svcData.getPartialError(i).getMessages()));
            }
            return false;
        }
        return true;
    }
}
