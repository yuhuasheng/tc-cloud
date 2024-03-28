package com.foxconn.plm.tcservice.config;

import java.lang.annotation.*;

/**
 * @author Robert
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TCPropertes {
    String tcProperty() default "";

    String tcType() default "";

    int cell() default -1;
}
