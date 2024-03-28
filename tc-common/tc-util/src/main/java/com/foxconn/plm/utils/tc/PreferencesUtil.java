package com.foxconn.plm.utils.tc;

import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.administration._2012_09.PreferenceManagement;
import com.teamcenter.soa.client.Connection;

public class PreferencesUtil {

    //获得指定名称首选项:站点类型
    public static String[] getTCPreferences(PreferenceManagementService preferenmanagementservice, String prefername) throws Exception {
        preferenmanagementservice.refreshPreferences();
        PreferenceManagement.GetPreferencesResponse getpreferencesRes = preferenmanagementservice.getPreferences(new String[]{prefername}, false);
        PreferenceManagement.CompletePreference[] completePref = getpreferencesRes.response;
        String[] temps = null;
        if (completePref.length > 0) {
            PreferenceManagement.CompletePreference onecompletePref = completePref[0];
            PreferenceManagement.PreferenceValue prefvalue = onecompletePref.values;
            temps = prefvalue.values;
        }
        return temps;
    }

    public static String[] getTCPreferences(Connection connection, String prefername) {
        PreferenceManagementService preferenmanagementservice = PreferenceManagementService.
                getService(connection);
        try {
            preferenmanagementservice.refreshPreferences();
        } catch (Exception e) {
            e.printStackTrace();
        }
        PreferenceManagement.GetPreferencesResponse getpreferencesRes = preferenmanagementservice.getPreferences(new String[]{prefername}, false);
        PreferenceManagement.CompletePreference[] completePref = getpreferencesRes.response;
        String[] temps = null;
        if (completePref.length > 0) {
            PreferenceManagement.CompletePreference onecompletePref = completePref[0];
            PreferenceManagement.PreferenceValue prefvalue = onecompletePref.values;
            temps = prefvalue.values;
        }
        return temps;
    }


}
