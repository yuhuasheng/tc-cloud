package com.foxconn.plm.integrate.lbs.domain;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SyncRp {

    private String rev;
    private String spasId;
    private String spasPhase;
    private String changList;
    private String projName;
    private MultipartFile excel;
    private MultipartFile zip;


}
