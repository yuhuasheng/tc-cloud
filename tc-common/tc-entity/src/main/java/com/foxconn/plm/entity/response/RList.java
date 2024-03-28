package com.foxconn.plm.entity.response;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RList<T>{

    String code;
    String msg;
    T data;

    @SuppressWarnings({"all"})
    public static <T> RList<T> ok(List<T> list, long total) {
        RList rList = new RList<>();
        rList.code = HttpResultEnum.SUCCESS.getCode();
        Map<String, Object> map = new HashMap<>();
        map.put("total",total);
        map.put("list",list);
        rList.data = map;
        rList.msg = "操作成功";
        return rList;

    }


}
