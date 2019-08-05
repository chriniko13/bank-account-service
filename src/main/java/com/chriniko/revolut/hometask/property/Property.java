package com.chriniko.revolut.hometask.property;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD, PARAMETER})
public @interface Property {

    /**
     * Key to search for in the property files.
     *
     * @return a string.
     */
    @Nonbinding String value() default "";

    /**
     * Is the key a mandatory key.
     *
     * @return true as default but false if set
     */
    @Nonbinding boolean required() default true;
}
