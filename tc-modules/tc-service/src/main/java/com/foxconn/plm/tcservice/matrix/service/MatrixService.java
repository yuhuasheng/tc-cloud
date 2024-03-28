package com.foxconn.plm.tcservice.matrix.service;

public interface MatrixService {

    String sendMatrixEmail(String taskName, String subject, String currentName, String changeDesc, String firstTargetUid,String attachments);
}
