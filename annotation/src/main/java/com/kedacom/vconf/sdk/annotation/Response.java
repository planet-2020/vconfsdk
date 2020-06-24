package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记一条响应。
 * 响应是由请求{@link Request}触发的，作为对比通知{@link Notification}是平台主动推过来的。
 * */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Response {
    String id();    // 响应ID
    Class clz();    // 响应内容（对应的类）
    int delay() default 500; // 触发该响应前的延时（单位：毫秒）。仅模拟模式有效。
}
