package com.foxconn.plm.ops.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author DY
 * @CLassName: MetaHandler
 * @Description:
 * @create 2022/10/7
 */
@Component
public class MetaHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.setFieldValByName("created", LocalDateTime.now(),metaObject);
        this.setFieldValByName("delFlag",0,metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("lastUpd", LocalDateTime.now(),metaObject);
    }
}
