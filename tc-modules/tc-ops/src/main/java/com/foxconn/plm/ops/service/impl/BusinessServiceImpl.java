package com.foxconn.plm.ops.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.JschUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.ops.entity.OperationEntity;
import com.foxconn.plm.ops.entity.OpsBatInfoEntity;
import com.foxconn.plm.ops.param.AddRecordParam;
import com.foxconn.plm.ops.param.DisableParam;
import com.foxconn.plm.ops.param.EditRecordParam;
import com.foxconn.plm.ops.param.SearchRecordParam;
import com.foxconn.plm.ops.response.SearchRecordRes;
import com.foxconn.plm.ops.service.BusinessService;
import com.foxconn.plm.ops.service.OperationService;
import com.foxconn.plm.ops.service.OpsBatInfoService;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:27
 **/
@Service
public class BusinessServiceImpl implements BusinessService {
    @Resource
    private OperationService operationService;
    @Resource
    private OpsBatInfoService opsBatInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R addRecord(AddRecordParam param) {
        // 查詢是否添加過
        OpsBatInfoEntity one = opsBatInfoService.getOne(new QueryWrapper<OpsBatInfoEntity>().lambda()
                .eq(OpsBatInfoEntity::getHost, param.getHost())
                .eq(OpsBatInfoEntity::getUserName, param.getUserName())
                .eq(OpsBatInfoEntity::getSecretKey, param.getSecretKey())
                .eq(OpsBatInfoEntity::getScriptPath, param.getScriptPath())
                .eq(OpsBatInfoEntity::getScript, param.getScript())
        );
        if(ObjectUtil.isNotNull(one)){
            return R.error("400","已經存在相同的配置");
        }
        one = new OpsBatInfoEntity();
        BeanUtil.copyProperties(param,one);
        one.setId(opsBatInfoService.getId());
        // 校驗腳本是否存在
        try {
            if (!checkScript(one)) {
                return R.error("400", "腳本在配置的路徑下不存在");
            }
        } catch (IORuntimeException| JschRuntimeException e){
            return R.error("400", "校驗腳本出錯，請確認賬號密碼是否正確");
        }
        return R.success(opsBatInfoService.save(one));
    }

    private Boolean checkScript(OpsBatInfoEntity entity){
        Session session = JschUtil.getSession(entity.getHost(), 22, entity.getUserName(), entity.getSecretKey());
        String exec = JschUtil.exec(session, "cd " + entity.getScriptPath() + "; ls;", CharsetUtil.CHARSET_UTF_8);
        List<String> split = StrSplitter.split(exec, "\n", true, true);
        return CollUtil.newHashSet(split).contains(entity.getScript());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R editRecord(EditRecordParam param) {
        // 查詢是否存在相同的數據
        OpsBatInfoEntity one = opsBatInfoService.getOne(new QueryWrapper<OpsBatInfoEntity>().lambda()
                .eq(OpsBatInfoEntity::getHost, param.getHost())
                .eq(OpsBatInfoEntity::getUserName, param.getUserName())
                .eq(OpsBatInfoEntity::getSecretKey, param.getSecretKey())
                .eq(OpsBatInfoEntity::getScriptPath, param.getScriptPath())
                .eq(OpsBatInfoEntity::getScript, param.getScript())
                .ne(OpsBatInfoEntity::getId,param.getId())
        );
        if(ObjectUtil.isNotNull(one)){
            return R.error("400","已經存在相同的配置");
        }
        OpsBatInfoEntity entity = new OpsBatInfoEntity();
        BeanUtil.copyProperties(param,entity);
        // 校驗腳本是否存在
        try {
            if (!checkScript(entity)) {
                return R.error("400", "腳本在配置的路徑下不存在");
            }
        } catch (IORuntimeException| JschRuntimeException e){
            return R.error("400", "校驗腳本出錯，請確認賬號密碼是否正確");
        }
        return R.success(opsBatInfoService.updateById(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R delRecord(List<String> ids) {
        List<OpsBatInfoEntity> list = opsBatInfoService.listByIds(ids);
        if(list.size() != ids.size()){
            return R.error("400","參數ids中存在錯誤的id");
        }
        return R.success(opsBatInfoService.removeByIds(ids));
    }

    @Override
    public RList<SearchRecordRes> searchRecord(SearchRecordParam param) {
        Page<OpsBatInfoEntity> page = new Page<>(param.getPageNum(),param.getPageSize());
        Page<OpsBatInfoEntity> iPage = opsBatInfoService.page(page, new QueryWrapper<OpsBatInfoEntity>()
                .like(StrUtil.isNotBlank(param.getHost()), "lower(host)", param.getHost())
                .like(StrUtil.isNotBlank(param.getUserName()), "lower(user_name)", param.getUserName())
                .like(StrUtil.isNotBlank(param.getSecretKey()), "lower(secret_key)", param.getSecretKey())
                .like(StrUtil.isNotBlank(param.getScriptPath()), "lower(script_path)", param.getScriptPath())
                .like(StrUtil.isNotBlank(param.getScript()), "lower(script)", param.getScript())
                .lambda()
                .orderByDesc(OpsBatInfoEntity::getId)
        );
        if(CollUtil.isEmpty(iPage.getRecords())){
            return RList.ok(Collections.emptyList(),0);
        }
        List<SearchRecordRes> collect = iPage.getRecords().stream().map(item -> {
            SearchRecordRes res = new SearchRecordRes();
            BeanUtil.copyProperties(item, res);
            return res;
        }).collect(Collectors.toList());
        return RList.ok(collect,iPage.getTotal());
    }

    @Override
    public R disable(DisableParam param) {
        OpsBatInfoEntity entity = opsBatInfoService.getById(param.getId());
        if(ObjectUtil.isNull(entity)){
            return R.error("400","參數數據id錯誤");
        }
        if(param.getDisable()){
            entity.setStatus("1");
        } else {
            entity.setStatus("0");
        }
        return R.success(opsBatInfoService.updateById(entity));
    }

    @Override
    public R execute(List<String> ids) {
        List<OpsBatInfoEntity> list = opsBatInfoService.listByIds(ids);
        if(list.size() != ids.size()){
            return R.error("400","參數ids中存在錯誤的id");
        }
        // 開始一項一項的執行
        for (OpsBatInfoEntity entity : list) {
            String cmd = entity.getScriptPath() + "/" + entity.getScript() + " restart;";
            Session session = JschUtil.getSession(entity.getHost(), 22, entity.getUserName(), entity.getSecretKey());
            String result = JschUtil.exec(session, cmd, CharsetUtil.CHARSET_UTF_8);
            // 添加記錄
            OperationEntity operationEntity = new OperationEntity();
            operationEntity.setId(operationService.getId());
            operationEntity.setBatInfoId(entity.getId());
            operationEntity.setResult(result);
            if(result.contains("start success")){
                operationEntity.setStatus("Y");
            }else{
                operationEntity.setStatus("N");
            }
            operationService.save(operationEntity);
        }
        return R.success(true);
    }
}
