package com.foxconn.plm.tcreport.drawcountreport.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foxconn.plm.tcreport.drawcountreport.domain.DrawCountEntity;
import com.foxconn.plm.tcreport.drawcountreport.service.DrawCountService;
import com.foxconn.plm.tcreport.mapper.DrawCountMapper;
import org.springframework.stereotype.Service;

/**
 * @ClassName: DrawCountServiceImpl
 * @Description:
 * @Author DY
 * @Create 2023/1/16
 */
@Service
public class DrawCountServiceImpl extends ServiceImpl<DrawCountMapper, DrawCountEntity> implements DrawCountService {
}
