package com.foxconn.plm.integrate.lbs.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.integrate.lbs.domain.SaveParam;
import com.foxconn.plm.integrate.lbs.domain.SyncRes;
import com.foxconn.plm.integrate.lbs.entity.LbsSyncEntity;
import com.foxconn.plm.integrate.lbs.mapper.LbsSyncMapper;
import com.foxconn.plm.integrate.lbs.service.LBSService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @ClassName: LBSServiceImpl
 * @Description:
 * @Author DY
 * @Create 2022/12/15
 */
@Service
public class LBSServiceImpl implements LBSService {
    @Resource
    private LbsSyncMapper mapper;
    @Resource
    private Snowflake snowflake;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveEntity(SaveParam param) {
        checkParam(param.getRev(), "rev不能為空");
        checkParam(param.getSpasPhase(), "spasPhase不能為空");
        checkParam(param.getChangList(), "changList不能為空");
        checkParam(param.getFileName(), "fileName不能為空");
        if (StrUtil.isBlank(param.getSpasId()) && StrUtil.isBlank(param.getProjName())) {
            throw new BizException(HttpResultEnum.SERVER_ERROR.getCode(), "spasId和projName不能同時為空");
        }
        // 文件必須是xlsx
        if (!param.getFileName().endsWith(".xls") && !param.getFileName().endsWith(".xlsx")) {
            throw new BizException(HttpResultEnum.PARAM_ERROR.getCode(), "Excel类型不对");
        }
        LbsSyncEntity entity = new LbsSyncEntity();
        BeanUtil.copyProperties(param, entity);
        entity.setId(snowflake.nextId());
        entity.setCreateTime(DateUtil.date());
        entity.setDelFlag("0");
        if (StrUtil.isBlank(param.getProjName())) {
            entity.setProjName("");
        }
        if (StrUtil.isBlank(param.getSpasId())) {
            entity.setSpasId("");
        }
        int i = mapper.saveEntity(entity);
        return i > 0;
    }

    @Override
    public List<SyncRes> getList() {
        return mapper.getList();
    }

    @Override
    public boolean batchDelete(List<String> ids) {
        int count = mapper.batchDelete(ids);
        return count == ids.size();
    }

    private void checkParam(String param, String errorMsg) {
        if (StrUtil.isBlank(param)) {
            throw new BizException(HttpResultEnum.SERVER_ERROR.getCode(), errorMsg);
        }
    }
}
