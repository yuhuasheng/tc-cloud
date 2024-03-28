package com.foxconn.plm.tcservice.issuemanagement.service;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.issuemanagement.param.AddDellIssueParam;
import com.foxconn.plm.tcservice.issuemanagement.param.AddHpIssueParam;
import com.foxconn.plm.tcservice.issuemanagement.param.AddIssueUpdatesParam;
import com.foxconn.plm.tcservice.issuemanagement.param.AddLenovoIssueParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 創建issue邏輯接口
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/19 16:38
 **/
public interface CreateIssueService {

    R createDellIssue(AddDellIssueParam param, List<MultipartFile> files);

    R createHpIssue(AddHpIssueParam param, List<MultipartFile> files);

    R createLenovoIssue(AddLenovoIssueParam param, List<MultipartFile> files);

    R addIssueUpdates(AddIssueUpdatesParam param, List<MultipartFile> files);
}
