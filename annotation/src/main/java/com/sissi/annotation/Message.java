package com.sissi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Sissi on 2018/9/5.
 */


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Message {
}
