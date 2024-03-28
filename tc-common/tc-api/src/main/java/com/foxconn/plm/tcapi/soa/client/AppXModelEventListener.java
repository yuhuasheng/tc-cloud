//==================================================
//
//  Copyright 2017 Siemens Product Lifecycle Management Software Inc. All Rights Reserved.
//
//==================================================

package com.foxconn.plm.tcapi.soa.client;


import com.teamcenter.soa.client.model.ModelEventListener;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.exceptions.NotLoadedException;

/**
 * Implementation of the ChangeListener. Print out all objects that have been updated.
 */
public class AppXModelEventListener extends ModelEventListener {

    @Override
    public void localObjectChange(ModelObject[] objects) {

    }

    @Override
    public void localObjectDelete(String[] uids) {


    }

}
