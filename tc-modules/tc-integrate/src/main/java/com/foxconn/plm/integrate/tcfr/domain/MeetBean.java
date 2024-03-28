package com.foxconn.plm.integrate.tcfr.domain;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.tcapi.serial.SerialCloneable;
import lombok.Data;

import java.util.List;

/**
 * @Author MW00333
 * @Date 2023/3/27 17:27
 * @Version 1.0
 */
@Data
public class MeetBean {

    private String spasProjId;
    private String spasProjPhase;
    private String spasSeries;
    private String meetMainOwner;
    private String customerName;
    private String meetingStartDate;
    private String meetingTitle;
    private String meetingMinutesPath;
    private String actionItemList;
    private String scheduleUid;
    private String projectName;
    private String meetingType;
    private String msg;
    private String uploadFLag;
    private String documentId;
    private String documentVer;
    private String documentUid;
    private String documentStatus;

    public MeetBean() {
    }

    public MeetBean(MeetInfo meetInfo) throws Exception {
        meetInfo.setSpasProjId(meetInfo.getSpasProjId().trim());
        meetInfo.setSpasProjPhase(meetInfo.getSpasProjPhase().trim());
        meetInfo.setSpasSeries(meetInfo.getSpasSeries().trim());
        meetInfo.setMeetMainOwner(meetInfo.getMeetMainOwner().trim());
        meetInfo.setCustomerName(meetInfo.getCustomerName().trim());
        meetInfo.setMeetingStartDate(meetInfo.getMeetingStartDate().trim());
        meetInfo.setMeetingTitle(meetInfo.getMeetingTitle().trim());
        meetInfo.setMeetingMinutesPath(meetInfo.getMeetingMinutesPath().trim());
        meetInfo.setMeetingType(meetInfo.getMeetingType().trim());

        this.spasProjId = meetInfo.getSpasProjId();
        this.spasProjPhase = meetInfo.getSpasProjPhase();
        this.spasSeries = meetInfo.getSpasSeries();
        this.meetMainOwner = meetInfo.getMeetMainOwner();
        this.customerName = meetInfo.getCustomerName();
        this.meetingStartDate = meetInfo.getMeetingStartDate();
        this.meetingTitle = meetInfo.getMeetingTitle();
        this.meetingMinutesPath = meetInfo.getMeetingMinutesPath();
        if ("NA".equals(meetInfo.getMeetingType())) {
            meetInfo.setMeetingType("");
        }
        this.meetingType = meetInfo.getMeetingType();
        List<MeetDataInfo> dataInfos = meetInfo.getData();
        if (CollUtil.isNotEmpty(dataInfos)) {
            this.actionItemList = JSONObject.toJSONString(dataInfos);
        }

    }


}
