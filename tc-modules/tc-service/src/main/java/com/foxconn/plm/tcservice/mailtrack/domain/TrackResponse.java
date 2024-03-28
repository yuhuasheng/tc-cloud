package com.foxconn.plm.tcservice.mailtrack.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TrackResponse {
    private String uid;
    private List<UserPojo> trackers = new ArrayList<>();
    private String stateName;
    private String itemId;
    private String itemRev;
    private PoInfo poInfo;
    private String workflowName;


}
