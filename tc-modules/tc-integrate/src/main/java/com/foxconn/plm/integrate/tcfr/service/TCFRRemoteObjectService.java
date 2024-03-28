package com.foxconn.plm.integrate.tcfr.service;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCAPIConstant;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.rma.RemoteBaseObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
public class TCFRRemoteObjectService extends  RemoteBaseObjectService {

    private static Log log = LogFactory.get();

    private ReentrantLock lock = new ReentrantLock(true); // 表示设置公平锁，所有线程老老实实排队

    @Autowired(required = false)
    ScheduleTaskService  scheduleTaskService;


    public R saveData(JSONObject paramJSONObject) throws Exception {
        try {
            lock.lock(); //获得锁
            return scheduleTaskService.saveTCData(paramJSONObject);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        } finally {
            lock.unlock(); // 释放锁
        }
    }


    @Override
    public String getObject() {
        return TCAPIConstant.TCFR;
    }
}
