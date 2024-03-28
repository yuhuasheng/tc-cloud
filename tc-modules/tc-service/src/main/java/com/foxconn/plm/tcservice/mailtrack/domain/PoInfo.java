package com.foxconn.plm.tcservice.mailtrack.domain;

import lombok.Data;

@Data
public class PoInfo {
    private String uid;
    private String stateName;
    private boolean needTrack;
    private String itemId;
    private long realDelayHours;
    private String urgcncy;
    private String dueDate;
    private String owner;
    private String itemRev;
    private String pmName;
    private String pmMail;
}
