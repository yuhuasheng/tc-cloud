package com.foxconn.dp.plm.hdfs.domain.rp;

import lombok.Data;

import java.util.List;

@Data
public class SendEmailRp {

    String projectName;
    String docName;
    String docUrl;
    List<Receiver> to;

    @Data
    public static class Receiver {
        String name;
        String email;
    }

}
