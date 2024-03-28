package com.foxconn.plm.integrate.spas.domain;

import com.teamcenter.soa.client.model.ModelObject;
import lombok.Data;

@Data
public class CustQueryResults {

    private int statusCode;
    private ModelObject[] objects;
    private String eerrorMessage;

}
