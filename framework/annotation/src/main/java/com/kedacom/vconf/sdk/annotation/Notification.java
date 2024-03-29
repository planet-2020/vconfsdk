package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记一条通知。
 * 通知是平台主动推过来的，作为对比响应{@link Response}是请求{@link Request}触发的。
 * */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Notification {
    String name() default "";  // 通知名称（若为空，可跟随同一元素上的@Response.name()）
    Class clz() default Default.class;    // 通知内容（若为Default，可跟随同一元素上的@Response.clz()）
    class Default{}
}
