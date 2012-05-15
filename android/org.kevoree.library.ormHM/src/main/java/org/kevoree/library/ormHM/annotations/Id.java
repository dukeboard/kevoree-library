package org.kevoree.library.ormHM.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 15/05/12
 * Time: 11:28
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    String attachTOCache() default "";
}
