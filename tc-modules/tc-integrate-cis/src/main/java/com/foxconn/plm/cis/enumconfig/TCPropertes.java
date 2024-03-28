package com.foxconn.plm.cis.enumconfig;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TCPropertes {
    String tcProperty() default "";
}
