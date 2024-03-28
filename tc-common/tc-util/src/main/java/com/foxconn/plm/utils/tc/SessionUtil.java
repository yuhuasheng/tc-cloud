package com.foxconn.plm.utils.tc;

import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.soa.client.model.Property;

public class SessionUtil {


    public static void byPass(SessionService sessionservice, boolean bypass) {
        com.teamcenter.services.loose.core._2007_12.Session.StateNameValue astatenamevalue[] = new com.teamcenter.services.loose.core._2007_12.Session.StateNameValue[1];
        astatenamevalue[0] = new com.teamcenter.services.loose.core._2007_12.Session.StateNameValue();
        astatenamevalue[0].name = "bypassFlag";
        astatenamevalue[0].value = Property.toBooleanString(bypass);
        sessionservice.setUserSessionState(astatenamevalue);
    }



}
