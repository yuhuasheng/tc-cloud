package com.foxconn.plm.integrate.tcfr.domain;

/**
 * @Author HuashengYu
 * @Date 2023/3/8 13:59
 * @Version 1.0
 */
public class TCFRConstant {

    public static final String TCFRFOLDER = "TCFR Report";

    public static final String FXN33_Schedule_Collaboration_Process = "FXN33_Schedule Collaboration Process";

    public static final String TCM_Release_Process = "TCM Release Process";

    public static final String FXN25_TCFR_FAST_RELEASE = "FXN25_TCFR Fast Release";

    public static final String SCHEDULETASKSTATUSREDISKEY = "schedule_task_status_redis_key"; // 记录状态值传递TCFR失败记录的缓存记录

    public static final String systemType = "S";

    public static final String documentType = "Meeting Minutes";

    public static final int DELIVERABLENAME = 256; // 任务交付件名称的长度

    public static final String TEMPLATEPATH = "tcfr\\SPAS_To_TCUser_Template.xlsx";

    public static final String SYNCPROJECTINFO = "syncProjectInfo";

    public static final String SYNCCUSTOMERLOV = "syncCustomerLov";

    public static final String SYNCMEETINGTYPELOV = "syncMeetingTypeLov";

    public static final String SYNCMEETINGFILEREDISKEY = "syncMeetingFileRedisKey";
}
