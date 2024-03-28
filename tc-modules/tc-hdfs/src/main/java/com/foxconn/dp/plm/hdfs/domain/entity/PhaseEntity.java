package com.foxconn.dp.plm.hdfs.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class PhaseEntity {

    Integer sid;
    Integer phaseId;
    String phaseSn;
    String phaseName;
    @JsonFormat(pattern = "yyyy/MM/dd", timezone = "GMT+8")
    Date startTime;
    @JsonFormat(pattern = "yyyy/MM/dd", timezone = "GMT+8")
    Date endTime;
}
