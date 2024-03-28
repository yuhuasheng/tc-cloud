package com.foxconn.plm.integrateb2b.dataExchange.factory;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.integrateb2b.dataExchange.constants.PlantConstants;
import com.foxconn.plm.integrateb2b.dataExchange.core.DataExchangeListener;
import com.foxconn.plm.integrateb2b.dataExchange.core.ext.MNTL10DataExchangeListener;
import com.foxconn.plm.integrateb2b.dataExchange.core.ext.MNTL5DataExchangeListener;
import com.foxconn.plm.integrateb2b.dataExchange.core.ext.MNTL6DataExchangeListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MNTDataExchangeFactory {

    private static Log log = LogFactory.get();
    @Autowired
    private MNTL6DataExchangeListener mntl6DataExchangeListener;

    @Autowired
    private MNTL10DataExchangeListener mntl10DataExchangeListener;


    @Autowired
    private MNTL5DataExchangeListener mntl5DataExchangeListener;



    public   DataExchangeListener getDataExchangeListener(String plant)throws Exception {
        if(PlantConstants.isContain(PlantConstants.MNT_L6,plant)){
            return mntl6DataExchangeListener;
        }else if(PlantConstants.isContain(PlantConstants.MNT_L10,plant)){
            return mntl10DataExchangeListener;
        }else if(PlantConstants.isContain(PlantConstants.MNT_L5,plant)){
            return mntl5DataExchangeListener;
        }
        return null;
    }

}
