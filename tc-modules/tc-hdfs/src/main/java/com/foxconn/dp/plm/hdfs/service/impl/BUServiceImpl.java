package com.foxconn.dp.plm.hdfs.service.impl;

import com.foxconn.dp.plm.hdfs.dao.xplm.BUMapper;
import com.foxconn.dp.plm.hdfs.domain.rp.SaveBuRp;
import com.foxconn.dp.plm.hdfs.domain.rv.LOVRv;
import com.foxconn.dp.plm.hdfs.domain.rv.ProductLineRv;
import com.foxconn.dp.plm.hdfs.service.BUService;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.utils.string.StringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Service
public class BUServiceImpl implements BUService {




    @Resource
    BUMapper mapper;

    @Override
    public List<BURv> getBUList(BUListRp rp) {
        return mapper.getBUList(rp);
    }




    @Override
    public List<LOVRv> getLovList(String name) {
        if ("customer".equals(name)) {
            return mapper.getCustomerList();
        }
        return null;
    }

    @Override
    public List<ProductLineRv> getProductLineList() {
        return mapper.getProductLineList();
    }

    @Override
    public void save(SaveBuRp rp) {
        int exits = mapper.exits(rp);
        if (exits != 0) {
            throw new BizException(HttpResultEnum.SERVER_ERROR.getCode(), "已经存在，无法保存");
        }
        if (StringUtil.isEmpty(rp.getId())) {
            // 新增
            mapper.insert(rp);
        } else {
            // 修改
            mapper.modify(rp);
        }
    }

    @Override
    public void delete(long id, String user) {
        mapper.delete(id, user);
    }
}
