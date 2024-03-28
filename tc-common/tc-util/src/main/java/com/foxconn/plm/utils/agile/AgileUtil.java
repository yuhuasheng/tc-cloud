package com.foxconn.plm.utils.agile;

import com.agile.api.APIException;
import com.agile.api.AgileSessionFactory;
import com.agile.api.IAgileSession;

import java.util.HashMap;
import java.util.Map;

public class AgileUtil {


    public static IAgileSession getAgileSession(String url,String userId,String pwd) {
        IAgileSession agileSession = null;
        Map<Integer, String> params = new HashMap<>();
        try {
            AgileSessionFactory factory = AgileSessionFactory.getInstance(url);
            params.put(AgileSessionFactory.PASSWORD, pwd);
            params.put(AgileSessionFactory.USERNAME, userId);
            agileSession = factory.createSession(params);
        } catch (APIException e) {
            e.printStackTrace();
        }
        return agileSession;
    }

}
