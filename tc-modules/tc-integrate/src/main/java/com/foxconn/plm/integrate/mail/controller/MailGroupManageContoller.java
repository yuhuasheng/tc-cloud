package com.foxconn.plm.integrate.mail.controller;

import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.integrate.mail.domain.ItemInfo;
import com.foxconn.plm.integrate.mail.domain.rp.MailGroupRp;
import com.foxconn.plm.integrate.mail.domain.rp.MailItemRp;
import com.foxconn.plm.integrate.mail.domain.rp.MailUserRp;
import com.foxconn.plm.integrate.mail.service.impl.MailGroupManageServiceImpl;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Scope("request")
@RequestMapping("/mailgroupmanage")
public class MailGroupManageContoller {

    private static Log log = LogFactory.get();
    @Autowired(required = false)
    private MailGroupManageServiceImpl mailGroupManageServiceImpl;


    @ApiOperation("向群組中添加物料")
    @PostMapping("/addGroupItems")
    public R<Long> addGroupItems(@RequestBody MailItemRp mailItem) {
        log.info("begin addGroupItems");
        log.info(JSONUtil.toJsonStr(mailItem));
        mailGroupManageServiceImpl.addMailItem(mailItem);
        log.info("end addGroupItems");
        return R.success(1l);
    }


    @ApiOperation("添加人员")
    @PostMapping("/addMailUser")
    public R<Long> addMailUser(@RequestBody MailUserRp mailUserRp) {
        log.info("begin addMailUser");
        log.info(JSONUtil.toJsonStr(mailUserRp));
        mailGroupManageServiceImpl.addMailUser(mailUserRp);
        log.info("end addMailUser");
        return R.success(0l);
    }


    @ApiOperation("查詢物料")
    @PostMapping("/searchItems")
    public RList<ItemInfo> searchItems(String keywords) {
        log.info("begin searchItems :" + keywords);
        if (keywords == null || keywords.equalsIgnoreCase("") || keywords.length() < 5) {
            throw new BizException("查询关键字过短，至少5个字符");
        }
        List<ItemInfo> ls = mailGroupManageServiceImpl.searchItems(keywords + "*");
        log.info("end searchItems");
        return RList.ok(ls, ls.size());
    }


    @ApiOperation("修改邮件群组")
    @PostMapping("/updateMailGroup")
    public R<Long> updateMailGroup(@RequestBody MailGroupRp mailGroup) {
        log.info("begin updateMailGroup");
        log.info("name:" + mailGroup.getGroupName() + ",description:" + mailGroup.getDescription());
        mailGroupManageServiceImpl.updateGroup(mailGroup);
        log.info("end updateMailGroup");
        return R.success(mailGroup.getId());
    }


    @ApiOperation("查詢物料類型")
    @PostMapping("/searchItemObjType")
    public RList<String> searchItemObjType() {
        log.info("begin searchItems");
        List<String> ls = mailGroupManageServiceImpl.searchItemObjType();
        log.info("end searchItems");
        return RList.ok(ls, ls.size());
    }


    @ApiOperation("创建邮件群组")
    @PostMapping("/addMailGroup")
    public R<Long> addMailGroup(@RequestBody MailGroupRp mailGroup) {
        log.info("begin addMailGroup");
        log.info("name:" + mailGroup.getGroupName() + ",description:" + mailGroup.getDescription());
        mailGroupManageServiceImpl.addMailGroup(mailGroup);
        log.info("end addMailGroup");
        return R.success(mailGroup.getId());
    }


}
