package com.foxconn.plm.integrate.tcfr.domain;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2023/3/7 17:20
 * @Version 1.0
 */
@Data
@ApiModel
public class MeetInfo {

    @ApiModelProperty(value = "请求类型")
    private String object = "";

    @ApiModelProperty(value = "请求的方法名")
    private String method = "";

    @ApiModelProperty(value = "Spas Id")
    private String spasProjId = "";

    @ApiModelProperty(value = "专案阶段")
    private String spasProjPhase = "";

    @ApiModelProperty(value = "系列")
    private String spasSeries = "";

    @ApiModelProperty(value = "会议日期")
    private String meetingStartDate = "";

    @ApiModelProperty(value = "会议标题")
    private String meetingTitle = "";

    @ApiModelProperty(value = "会议附件路径")
    private String meetingMinutesPath = "";

    @ApiModelProperty(value = "客户名称")
    private String customerName = "";

    @ApiModelProperty(value = "会议纪要主负责人")
    private String meetMainOwner = "";

    @ApiModelProperty(value = "会议类型")
    private String meetingType = "";

    private List<MeetDataInfo> data;


    public static MeetInfo propMapping(MeetBean rootBean) {
        MeetInfo meetInfo = new MeetInfo();
        meetInfo.setSpasProjId(rootBean.getSpasProjId());
        meetInfo.setSpasProjPhase(rootBean.getSpasProjPhase());
        meetInfo.setSpasSeries(rootBean.getSpasSeries());
        meetInfo.setMeetingStartDate(rootBean.getMeetingStartDate());
        meetInfo.setMeetingTitle(rootBean.getMeetingTitle());
        meetInfo.setMeetingMinutesPath(rootBean.getMeetingMinutesPath());
        meetInfo.setCustomerName(rootBean.getCustomerName());
        meetInfo.setMeetMainOwner(rootBean.getMeetMainOwner());
        meetInfo.setMeetingType(rootBean.getMeetingType());
        List<MeetDataInfo> meetDataInfos = JSONObject.parseArray(rootBean.getActionItemList(), MeetDataInfo.class);
        meetInfo.setData(meetDataInfos);
        return meetInfo;
    }
}
