package com.foxconn.plm.tcservice.mailmonitor.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.mapper.master.MailMonitorMapper;
import com.foxconn.plm.utils.collect.CollectUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/7/21 11:02
 * @Version 1.0
 */
@RestController
@RequestMapping("/teamcenter")
public class MailController {
    private static Log log = LogFactory.get();
    @Resource
    private MailMonitorMapper mailCleanupMapper;

    @ApiOperation("获取TC非活动状态的邮箱列表")
    @GetMapping("/getActiveEmail")
    public R getActiveEmail() {
        try {
            List<Map> list = mailCleanupMapper.getActiveEmail();// 获取活动状态的邮箱列表
            if (CollectUtil.isEmpty(list)) {
                return R.success("未查询到活动状态的TC邮箱记录");
            }
            list.removeIf(e -> e.get("email") == null || "".equals(e.get("email").toString().trim())); // 移除邮箱为空的记录
            log.info("==>> 活动状态TC邮箱记录列表: " + list);
            return R.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        }
    }
}
