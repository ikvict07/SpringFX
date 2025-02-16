package org.nevertouchgrass.springfx.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Constraints {
    double top() default -1;

    double right() default -1;

    double bottom() default -1;

    double left() default -1;
}
