package com.foxconn.plm.entity.constants;

/**
 * @Author HuashengYu
 * @Date 2023/3/8 10:53
 * @Version 1.0
 */
public class TCScheduleConstant {

    public static final String PROPERTY_OBJECT_TYPE_SCHEDULETYPE = "Schedule";
    public static final String PROPERTY_OBJECT_TYPE_SCHEDULETASKTYPE = "ScheduleTask";

    public static final String PROPERTY_FND0STATE = "fnd0state";

    public static final String PROPERTY_OBJECT_NAME = "object_name";
    public static final String PROPERTY_OBJECT_DESC = "object_desc";
    public final static String PROPERTY_D9_ACTUALUSERID = "d9_ActualUserID";
    public final static String PROPERTY_D9_REALAUTHOR = "d9_RealAuthor";

    public static final String STATE_CLOSED = "closed";
    public static final String STATE_COMPLETE = "complete";

    public static final String REL_RESOURCEASSIGNMENT = "ResourceAssignment";
    public static final String REL_FND0SUMMARYTASK = "fnd0SummaryTask";
    public static final String REL_CHILD_TASK_TAGLIST = "child_task_taglist";
    public static final String REL_SCHEDULE_DELIVERABLE_LIST = "schedule_deliverable_list";
    public static final String REL_SCH_TASK_DELIVERABLE_LIST = "sch_task_deliverable_list";
    public static final String REL_OWNERING_USER = "owning_user";
}
