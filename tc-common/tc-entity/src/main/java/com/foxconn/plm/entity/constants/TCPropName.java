package com.foxconn.plm.entity.constants;

import java.lang.annotation.*;

/**
 * @Author HuashengYu
 * @Date 2022/7/22 9:00
 * @Version 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TCPropName {

    /**
     * 对应的 TC 属性Key
     */
    String value() default "";

    String otherVal() default "";

    /**
     * 用户判断BONLine 是否合并的联合主键
     */
    boolean isKey() default true;

    /**
     * 用户判断BONLine 是否需要处理的字段
     */
    boolean isProcessField() default false;

    /**
     * 定义字段输入顺序
     */
    int order() default 1;

    /**
     * 定义字段是否必填
     */
    boolean isRequire() default false;

    int cell() default -1;

    int row() default -1;

    String tcProperty() default "";

    String tcType() default "";

    boolean isMerge() default false;
}
