package com.foxconn.plm.integrate.tcfr.annotation;

import java.lang.annotation.*;

/**
 * @Author MW00333
 * @Date 2023/4/10 11:44
 * @Version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessLimit {
    int seconds();
    int minutes();
    int maxCount();
}
