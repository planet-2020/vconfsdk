package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记消息定义枚举类.
 *
 * Created by Sissi on 2018/9/5.
 */


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Message {
//    /** 消息前缀。
//    * 消息前缀需保证全局唯一，否则编译报错。*/
//    String prefix() default "";
}
