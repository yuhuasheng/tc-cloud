package com.foxconn.dp.plm.hdfs.service;


import com.foxconn.dp.plm.hdfs.domain.entity.TCProjectEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.ProjectListRp;
import com.foxconn.plm.entity.response.R;

import java.util.List;

public interface ProjectService {

    public R<List<TCProjectEntity>> getProjectsByEmpId(ProjectListRp rp);


}
