package com.foxconn.plm.integrate.cis.config;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TCPropertes {
    String tcProperty() default "";
}
