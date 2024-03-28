package com.foxconn.plm.tcserviceawc.service;

import com.foxconn.plm.tcserviceawc.param.PersonalFolderParam;
import com.foxconn.plm.tcserviceawc.param.TaskUidsParam;

import java.util.List;

/**
 * 個人工作區業務邏輯接口類
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/23 17:07
 **/
public interface PersonalFolderService {
    String getPersonalFolderUid(PersonalFolderParam param);

    String getTaskFolderUid(PersonalFolderParam param);

    List<String> getTaskUids(TaskUidsParam param);
}
